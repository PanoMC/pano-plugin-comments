package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.NoPermission
import com.panomc.platform.error.NotFound
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.CommentLikeDao
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema

@Endpoint
class DeleteCommentAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val commentLikeDao: CommentLikeDao
) : LoggedInApi() {
    override val paths = listOf(Path("/api/comments/:id", RouteType.DELETE))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(param("id", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        val id = context.pathParam("id").toLong()

        val sqlClient = databaseManager.getSqlClient()
        val comment = commentDao.getById(id, sqlClient) ?: return NotFound()

        val userId = authProvider.getUserIdFromRoutingContext(context)

        if (comment.userId != userId) {
            return NoPermission()
        }

        val subComments = commentDao.getRepliesByParentId(id, null, null, sqlClient)

        commentDao.deleteById(id, sqlClient) // delete post

        subComments.forEach {
            commentLikeDao.deleteByCommentId(it.id, sqlClient) // delete sub comment likes
        }

        commentLikeDao.deleteByCommentId(id, sqlClient) // delete comment likes
        commentDao.deleteByParentId(id, sqlClient) // delete sub comments

        return Successful()
    }
}
