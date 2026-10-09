package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.BadRequest
import com.panomc.platform.error.NotFound
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.config.CommentsConfig
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.CommentLikeDao
import com.panomc.plugins.comments.db.model.CommentLike
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.RequestPredicate
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Bodies.json
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import com.panomc.platform.schema.EndpointDoc
import io.vertx.json.schema.common.dsl.Schemas.*
import com.panomc.platform.error.NotLoggedIn

@Endpoint
class ToggleLikeAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val commentLikeDao: CommentLikeDao
) : LoggedInApi() {
    override val paths = listOf(Path("/comments/like/toggle", RouteType.POST))

    override val doc = EndpointDoc(
        summary = "Likes a comment, or takes the like back.",
        tag = "comments",
        response = objectSchema()
            .requiredProperty("likes", intSchema())
            .requiredProperty("isLiked", booleanSchema()),
        errors = listOf(BadRequest::class, NotLoggedIn::class, NotFound::class)
    )

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
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
                        .requiredProperty("commentId", numberSchema())
                )
            )
            .predicate(RequestPredicate.BODY_REQUIRED)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        if (!configManager.config.likesEnabled) {
            return BadRequest()
        }

        val commentId = context.body().asJsonObject().getLong("commentId")

        val sqlClient = databaseManager.getSqlClient()

        val userId = authProvider.getUserIdFromRoutingContext(context)

        if (commentDao.getById(commentId, sqlClient) == null) {
            throw NotFound()
        }

        val existingLike = commentLikeDao.getByCommentIdAndUserId(commentId, userId, sqlClient)

        if (existingLike == null) {
            // Like
            commentLikeDao.add(CommentLike(commentId = commentId, userId = userId), sqlClient)
        } else {
            // Unlike
            commentLikeDao.deleteByCommentIdAndUserId(commentId, userId, sqlClient)
        }

        // Update comment like count
        val newCount = commentLikeDao.countByCommentId(commentId, sqlClient).toInt()
        commentDao.updateLikes(commentId, newCount, sqlClient)

        return Successful(mapOf("likes" to newCount, "isLiked" to (existingLike == null)))
    }
}
