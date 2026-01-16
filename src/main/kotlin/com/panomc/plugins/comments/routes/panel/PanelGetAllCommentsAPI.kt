package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.auth.PermissionManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.PageNotFound
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.optionalParam
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema
import kotlin.math.ceil

@Endpoint
class PanelGetAllCommentsAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/comments", RouteType.GET))

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
            .queryParameter(optionalParam("page", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)

        val parameters = getParameters(context)
        val page = parameters.queryParameter("page")?.long ?: 1L

        val limit = 10L
        val sqlClient = databaseManager.getSqlClient()
        val comments = commentDao.getAllRoot(page, limit, sqlClient)
        val count = commentDao.countRoot(sqlClient)

        val replyCounts = commentDao.getReplyCounts(comments.map { it.id }, sqlClient)

        val totalPageNum = ceil(count.toDouble() / limit).toLong()

        if (totalPageNum in 1..<page) {
            throw PageNotFound()
        }

        val resultComments = run {
            val userIds = comments.mapNotNull { it.userId }.toSet().toList()
            val usernames = databaseManager.userDao.getUsernameByListOfId(userIds, sqlClient)

            comments.map { comment ->
                val map = JsonObject.mapFrom(comment).map
                map["replyCount"] = replyCounts[comment.id] ?: 0
                map["username"] = comment.userId?.let { usernames[it] } ?: "-"
                val userGroup = comment.userId?.let { permissionManager.getPermissionGroup(it) }
                map["userGroup"] = if (userGroup != null) mapOf(
                    "name" to userGroup.name,
                    "displayName" to userGroup.displayName
                ) else null
                map
            }
        }

        return Successful(
            mapOf(
                "comments" to resultComments,
                "totalPage" to totalPageNum,
                "commentCount" to count
            )
        )
    }
}
