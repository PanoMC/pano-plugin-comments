package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.config.CommentsConfig
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository

@Endpoint
class GetConfigAPI(
    private val plugin: CommentsPlugin
) : Api() {
    override val paths = listOf(Path("/api/comment/config", RouteType.GET))

    private val configManager by lazy {
        plugin.pluginBeanContext.getBean(PluginConfigManager::class.java) as PluginConfigManager<CommentsConfig>
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        return Successful(JsonObject.mapFrom(configManager.config).map)
    }
}
