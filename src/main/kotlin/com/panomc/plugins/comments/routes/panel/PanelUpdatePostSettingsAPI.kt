package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.PostCommentSettingsDao
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.Parameters.param
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class PanelUpdatePostSettingsAPI(
    private val plugin: CommentsPlugin
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/comment/settings/:postId", RouteType.PUT))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val postCommentSettingsDao by lazy {
        plugin.pluginBeanContext.getBean(PostCommentSettingsDao::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(param("postId", numberSchema()))
            .body(
                Bodies.json(
                    objectSchema()
                        .requiredProperty("enabled", booleanSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)

        val postId = context.pathParam("postId").toLong()
        val body = context.body().asJsonObject()
        val enabled = body.getBoolean("enabled")

        val sqlClient = databaseManager.getSqlClient()

        postCommentSettingsDao.set(postId, enabled, sqlClient)

        return Successful()
    }
}
