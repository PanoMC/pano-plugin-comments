package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.auth.PermissionManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.platform.error.NotFound
import com.panomc.platform.util.PostStatus
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.util.PageInfo
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.CommentLikeDao
import com.panomc.plugins.comments.db.dao.PostCommentSettingsDao
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
class GetCommentsAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val commentLikeDao: CommentLikeDao,
    private val postCommentSettingsDao: PostCommentSettingsDao
) : Api() {
    override val paths = listOf(Path("/comments/:postId", RouteType.GET))

    override val doc = EndpointDoc(
        summary = "The top-level comments of a post, ten per page, each with its first replies.",
        tag = "comments",
        response = objectSchema()
            .requiredProperty("items", arraySchema().items(CommentSchemas.comment(withReplies = true)))
            .requiredProperty("page", CommentSchemas.page())
            .requiredProperty("commentsEnabled", booleanSchema()),
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
            .pathParameter(param("postId", numberSchema()))
            .queryParameter(optionalParam("page", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val postId = context.pathParam("postId").toLong()
        val page = context.queryParam("page").firstOrNull()?.toLong() ?: 1L
        val limit = 10L

        val loggedIn = authProvider.isLoggedIn(context)
        val sqlClient = databaseManager.getSqlClient()

        // Comments of a draft or trashed post stay hidden, like the post itself.
        val post = databaseManager.postDao.getById(postId, sqlClient)
        if (post == null || post.status != PostStatus.PUBLISHED) {
            throw NotFound()
        }

        val totalComments = commentDao.countRootByPostId(postId, sqlClient)
        val comments = commentDao.getRootByPostId(postId, page, limit, sqlClient)
        val replyCounts = commentDao.getReplyCounts(comments.map { it.id }, sqlClient)

        val userId = if (loggedIn) authProvider.getUserIdFromRoutingContext(context) else null
        val resultComments = run {
            val commentsWithReplies = comments.map { it to commentDao.getRepliesByParentId(it.id, null, 3, sqlClient) }
            val allUserIds = (comments.mapNotNull { it.userId } + commentsWithReplies.flatMap { (_, r) -> r.mapNotNull { it.userId } }).toSet().toList()
            val usernames = if (allUserIds.isEmpty()) emptyMap() else databaseManager.userDao.getUsernameByListOfId(allUserIds, sqlClient)

            commentsWithReplies.map { (comment, replies) ->
                val isLiked = if (userId != null) {
                    commentLikeDao.getByCommentIdAndUserId(comment.id, userId, sqlClient) != null
                } else false

                val resultReplies = replies.map { reply ->
                    val replyIsLiked = if (userId != null) {
                        commentLikeDao.getByCommentIdAndUserId(reply.id, userId, sqlClient) != null
                    } else false

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
                        "isLiked" to replyIsLiked,
                        "userGroup" to if (replyUserGroup != null) mapOf(
                            "name" to replyUserGroup.name,
                            "displayName" to replyUserGroup.displayName
                        ) else null
                    )
                }

                val userGroup = comment.userId?.let { permissionManager.getPermissionGroup(it) }

                mapOf(
                    "id" to comment.id,
                    "postId" to comment.postId,
                    "userId" to comment.userId,
                    "username" to (comment.userId?.let { usernames[it] } ?: "-"),
                    "content" to comment.content,
                    "likes" to comment.likes,
                    "parentId" to comment.parentId,
                    "createdAt" to comment.createdAt,
                    "updatedAt" to comment.updatedAt,
                    "isLiked" to isLiked,
                    "replyCount" to (replyCounts[comment.id] ?: 0),
                    "replies" to resultReplies,
                    "userGroup" to if (userGroup != null) mapOf(
                        "name" to userGroup.name,
                        "displayName" to userGroup.displayName
                    ) else null
                )
            }
        }

        val settings = postCommentSettingsDao.get(postId, sqlClient)

        return Successful(
            mapOf(
                "items" to resultComments,
                "page" to PageInfo.of(page, limit.toInt(), totalComments),
                "commentsEnabled" to (settings?.enabled ?: true)
            )
        )
    }
}
