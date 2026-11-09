package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.auth.PermissionManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.platform.error.NotFound
import com.panomc.platform.util.PostStatus
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.CommentLikeDao
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters.optionalParam
import com.panomc.platform.schema.dsl.Parameters.param
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema
import com.panomc.platform.schema.EndpointDoc
import io.vertx.json.schema.common.dsl.Schemas.*
import com.panomc.plugins.comments.util.CommentSchemas

@Endpoint
class GetRepliesAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val commentLikeDao: CommentLikeDao
) : Api() {
    override val paths = listOf(Path("/comments/replies/:commentId", RouteType.GET))

    override val doc = EndpointDoc(
        summary = "The next replies of a comment, three at a time; lastId is the id of the last reply already shown.",
        tag = "comments",
        response = objectSchema()
            .requiredProperty("items", arraySchema().items(CommentSchemas.comment(withReplies = false))),
        errors = listOf(NotFound::class)
    )

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val permissionManager by lazy {
        plugin.applicationContext.getBean(PermissionManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(param("commentId", numberSchema()))
            .queryParameter(optionalParam("lastId", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val commentId = context.pathParam("commentId").toLong()
        val lastId = context.queryParam("lastId").firstOrNull()?.toLong()
        val limit = 3

        val loggedIn = authProvider.isLoggedIn(context)
        val sqlClient = databaseManager.getSqlClient()

        // Replies of a draft or trashed post stay hidden, like the post itself.
        val parent = commentDao.getById(commentId, sqlClient) ?: throw NotFound()
        val post = databaseManager.postDao.getById(parent.postId, sqlClient)
        if (post == null || post.status != PostStatus.PUBLISHED) {
            throw NotFound()
        }

        val replies = commentDao.getRepliesByParentId(commentId, lastId, limit, sqlClient)

        val resultReplies = run {
            val userIds = replies.mapNotNull { it.userId }.toSet().toList()
            val usernames = if (userIds.isEmpty()) emptyMap() else databaseManager.userDao.getUsernameByListOfId(userIds, sqlClient)

            replies.map { reply ->
                var isLiked = false

                if (loggedIn) {
                    val userId = authProvider.getUserIdFromRoutingContext(context)
                    isLiked = commentLikeDao.getByCommentIdAndUserId(reply.id, userId, sqlClient) != null
                }

                val replyUserGroup = reply.userId?.let { permissionManager.getPermissionGroup(it) }

                mapOf(
                    "id" to reply.id,
                    "postId" to reply.postId,
                    "userId" to reply.userId,
                    "username" to (reply.userId?.let { usernames[it] } ?: "-"),
                    "content" to reply.content,
                    "likes" to reply.likes,
                    "parentId" to reply.parentId,
                    "createdAt" to reply.createdAt,
                    "updatedAt" to reply.updatedAt,
                    "isLiked" to isLiked,
                    "userGroup" to if (replyUserGroup != null) mapOf(
                        "name" to replyUserGroup.name,
                        "displayName" to replyUserGroup.displayName
                    ) else null
                )
            }
        }

        return Successful(
            mapOf(
                "items" to resultReplies
            )
        )
    }
}
