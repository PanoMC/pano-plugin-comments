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
import com.panomc.plugins.license.LicenseGuard
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies.json
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class AddCommentAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val postCommentSettingsDao: PostCommentSettingsDao
) : LoggedInApi() {
    override val paths = listOf(Path("/api/comments", RouteType.POST))

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
        // Anti-tamper: re-assert at a critical write path. Patching out only the
        // onStart check is not enough to crack the plugin if 5+ call sites assert.
        LicenseGuard.assert(plugin)

        val body = context.body().asJsonObject()

        val postId = body.getLong("postId")
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
            if (parentComment == null || parentComment.parentId != null) {
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

        if (configManager.config.cooldown > 0) {
            val lastComment = if (parentId != null) {
                commentDao.getLastReplyByUserId(userId, sqlClient)
            } else {
                commentDao.getLastRootCommentByUserId(userId, sqlClient)
            }
            if (lastComment != null) {
                val diff = System.currentTimeMillis() - lastComment.createdAt
                if (diff < configManager.config.cooldown * 1000L) {
                    throw RateLimited()
                }
            }
        }

        val comment = Comment(
            postId = postId,
            userId = userId,
            content = body.getString("content"),
            parentId = parentId
        )

        val id = commentDao.add(comment, sqlClient)
        comment.id = id

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
