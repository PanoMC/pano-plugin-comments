package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.auth.PermissionManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.BadRequest
import com.panomc.platform.error.NoPermission
import com.panomc.platform.error.NotFound
import com.panomc.platform.error.RateLimited
import com.panomc.platform.model.*
import com.panomc.platform.util.PostStatus
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.config.CommentsConfig
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.PostCommentSettingsDao
import com.panomc.plugins.comments.db.model.Comment
import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import java.util.concurrent.ConcurrentHashMap
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Bodies.json
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.*
import com.panomc.platform.schema.EndpointDoc
import com.panomc.platform.error.NotLoggedIn
import com.panomc.plugins.comments.util.CommentSchemas

@Endpoint
class AddCommentAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val postCommentSettingsDao: PostCommentSettingsDao
) : LoggedInApi() {
    override val paths = listOf(Path("/comments", RouteType.POST))

    override val doc = EndpointDoc(
        summary = "Writes a comment on a post, or a reply when parentId is given.",
        tag = "comments",
        response = objectSchema()
            .requiredProperty("comment", CommentSchemas.comment(withReplies = false)),
        errors = listOf(BadRequest::class, NotLoggedIn::class, NoPermission::class, NotFound::class, RateLimited::class)
    )

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val permissionManager by lazy {
        plugin.applicationContext.getBean(PermissionManager::class.java)
    }

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<CommentsConfig>
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    companion object {
        const val MAX_CONTENT_LENGTH = 5000

        // A comment body is a few KB at most; anything bigger is refused before it is read.
        private const val BODY_LIMIT = 64L * 1024

        // Users with a request in progress, so parallel requests cannot slip past the cooldown together.
        private val postingUsers = ConcurrentHashMap.newKeySet<Long>()
    }

    override fun bodyHandler(): Handler<RoutingContext> =
        BodyHandler.create()
            .setDeleteUploadedFilesOnEnd(true)
            .setBodyLimit(BODY_LIMIT)

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                json(
                    objectSchema()
                        .requiredProperty("postId", numberSchema())
                        .requiredProperty("content", stringSchema())
                        .optionalProperty("parentId", numberSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val body = context.body().asJsonObject()

        val postId = body.getLong("postId")
        val content = body.getString("content")?.trim()

        if (content.isNullOrEmpty() || content.length > MAX_CONTENT_LENGTH) {
            throw BadRequest()
        }

        val sqlClient = databaseManager.getSqlClient()

        val post = databaseManager.postDao.getById(postId, sqlClient) ?: throw NotFound()

        if (post.status != PostStatus.PUBLISHED) {
            throw NotFound()
        }

        val settings = postCommentSettingsDao.get(postId, sqlClient)
        if (settings != null && !settings.enabled) {
            throw NoPermission("Comments are disabled for this post")
        }

        val userId = authProvider.getUserIdFromRoutingContext(context)

        val username = databaseManager.userDao.getUsernameFromUserId(userId, sqlClient)!!

        val parentId = if (body.containsKey("parentId")) body.getLong("parentId") else null

        if (parentId != null) {
            if (!configManager.config.repliesEnabled) {
                return BadRequest()
            }

            if (configManager.config.replyPermission.isNotEmpty() && !permissionManager.hasPermissionNode(
                    userId,
                    configManager.config.replyPermission
                )
            ) {
                throw NoPermission()
            }

            val parentComment = commentDao.getById(parentId, sqlClient)
            // A reply must stay on the post of its parent, so the checks above apply to the thread it lands in.
            if (parentComment == null || parentComment.parentId != null || parentComment.postId != postId) {
                return BadRequest()
            }
        } else {
            if (configManager.config.commentPermission.isNotEmpty() && !permissionManager.hasPermissionNode(
                    userId,
                    configManager.config.commentPermission
                )
            ) {
                throw NoPermission()
            }
        }

        val cooldown = configManager.config.cooldown

        if (cooldown > 0 && !postingUsers.add(userId)) {
            throw RateLimited()
        }

        val comment = try {
            if (cooldown > 0) {
                val lastComment = if (parentId != null) {
                    commentDao.getLastReplyByUserId(userId, sqlClient)
                } else {
                    commentDao.getLastRootCommentByUserId(userId, sqlClient)
                }
                if (lastComment != null) {
                    val diff = System.currentTimeMillis() - lastComment.createdAt
                    if (diff < cooldown * 1000L) {
                        throw RateLimited()
                    }
                }
            }

            val newComment = Comment(
                postId = postId,
                userId = userId,
                content = content,
                parentId = parentId
            )

            newComment.id = commentDao.add(newComment, sqlClient)

            newComment
        } finally {
            if (cooldown > 0) {
                postingUsers.remove(userId)
            }
        }

        return Successful(
            mapOf(
                "comment" to mapOf(
                    "id" to comment.id,
                    "postId" to comment.postId,
                    "userId" to comment.userId,
                    "username" to username,
                    "content" to comment.content,
                    "likes" to comment.likes,
                    "parentId" to comment.parentId,
                    "createdAt" to comment.createdAt,
                    "updatedAt" to comment.updatedAt,
                    "isLiked" to false,
                    "replyCount" to 0,
                    "userGroup" to permissionManager.getPermissionGroup(userId)?.let {
                        mapOf(
                            "name" to it.name,
                            "displayName" to it.displayName
                        )
                    }
                )
            )
        )
    }
}
