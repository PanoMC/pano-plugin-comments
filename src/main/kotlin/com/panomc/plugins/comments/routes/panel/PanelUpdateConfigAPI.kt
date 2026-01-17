package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.config.CommentsConfig
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Bodies
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class PanelUpdateConfigAPI(
    private val plugin: CommentsPlugin,
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/comment/config", RouteType.PUT))

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<CommentsConfig>
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .body(
                Bodies.json(
                    objectSchema()
                        .optionalProperty("repliesEnabled", booleanSchema())
                        .optionalProperty("likesEnabled", booleanSchema())
                        .optionalProperty("paginationType", stringSchema())
                        .optionalProperty("commentPermission", stringSchema())
                        .optionalProperty("replyPermission", stringSchema())
                        .optionalProperty("cooldown", intSchema())
                        .optionalProperty("inputStyle", stringSchema())
                )
            )
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)

        val body = context.body().asJsonObject()
        configManager.saveConfig(body)

        return Successful()
    }
}
