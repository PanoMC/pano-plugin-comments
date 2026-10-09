package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.auth.PermissionManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters.optionalParam
import com.panomc.platform.schema.dsl.Parameters.param
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema

@Endpoint
class PanelGetRepliesAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao
) : PanelApi() {
    override val paths = listOf(Path("/comments/:id/replies", RouteType.GET))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val permissionManager by lazy {
        plugin.applicationContext.getBean(PermissionManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(param("id", numberSchema()))
            .queryParameter( optionalParam("lastId", numberSchema()))
            .queryParameter(optionalParam("limit", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)

        val id = context.pathParam("id").toLong()
        val lastId = context.queryParam("lastId").firstOrNull()?.toLongOrNull()
        val limit = context.queryParam("limit").firstOrNull()?.toIntOrNull() ?: 10

        val sqlClient = databaseManager.getSqlClient()
        val replies = commentDao.getRepliesByParentId(id, lastId, limit, sqlClient = sqlClient)

        val resultReplies = run {
            val userIds = replies.mapNotNull { it.userId }.toSet().toList()
            val usernames = databaseManager.userDao.getUsernameByListOfId(userIds, sqlClient)

            replies.map { reply ->
                val map = JsonObject.mapFrom(reply).map
                map["username"] = reply.userId?.let { usernames[it] } ?: "-"
                val userGroup = reply.userId?.let { permissionManager.getPermissionGroup(it) }
                map["userGroup"] = if (userGroup != null) mapOf(
                    "name" to userGroup.name,
                    "displayName" to userGroup.displayName
                ) else null
                map
            }
        }

        return Successful(
            mapOf(
                "items" to resultReplies
            )
        )
    }
}
