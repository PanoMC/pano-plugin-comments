package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.CommentLikeDao
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters.param
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema

@Endpoint
class PanelDeleteCommentAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val commentLikeDao: CommentLikeDao
) : PanelApi() {
    override val paths = listOf(Path("/comments/:id", RouteType.DELETE))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(param("id", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)
        val id = context.pathParam("id").toLong()
        val sqlClient = databaseManager.getSqlClient()
        
        // Same cascade as the user-side delete: the replies and every like go with the comment.
        val replies = commentDao.getRepliesByParentId(id, null, null, sqlClient)

        replies.forEach {
            commentLikeDao.deleteByCommentId(it.id, sqlClient)
        }

        commentDao.deleteByParentId(id, sqlClient)
        commentLikeDao.deleteByCommentId(id, sqlClient)
        commentDao.deleteById(id, sqlClient)

        return Successful()
    }
}
