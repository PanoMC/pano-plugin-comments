package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters.param
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

@Endpoint
class PanelGetPostCommentCountsAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao
) : PanelApi() {
    override val paths = listOf(Path("/comment/post-counts", RouteType.GET))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .queryParameter(param("postIds", Schemas.arraySchema().items(stringSchema())))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)

        val postIdsStr = context.queryParam("postIds").firstOrNull() ?: ""
        if (postIdsStr.isBlank()) {
            return Successful(mapOf("counts" to emptyMap<Long, Int>()))
        }

        val postIds = postIdsStr.split(",").mapNotNull { it.trim().toLongOrNull() }
        if (postIds.isEmpty()) {
            return Successful(mapOf("counts" to emptyMap<Long, Int>()))
        }

        val sqlClient = databaseManager.getSqlClient()
        val counts = commentDao.getCommentCounts(postIds, sqlClient)

        return Successful(mapOf("counts" to counts))
    }
}
