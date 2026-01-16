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
import io.vertx.ext.web.validation.builder.Parameters.optionalParam
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema

@Endpoint
class PanelGetCommentsAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/comments/:postId", RouteType.GET))

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
            .pathParameter(param("postId", numberSchema()))
            .queryParameter(optionalParam("commentsPage", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)
        val postId = context.pathParam("postId").toLong()
        val parameters = getParameters(context)
        val page = parameters.queryParameter("commentsPage")?.long ?: 1L
        val limit = 10L

        val sqlClient = databaseManager.getSqlClient()
        val totalComments = commentDao.countRootByPostId(postId, sqlClient)
        val totalPages = if (totalComments == 0L) 1L else Math.ceil(totalComments.toDouble() / limit).toLong()
        val comments = commentDao.getRootByPostId(postId, page, limit, sqlClient)
        val replyCounts = commentDao.getReplyCounts(comments.map { it.id }, sqlClient)

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
                "totalPage" to totalPages,
                "totalComments" to totalComments
            )
        )
    }
}
