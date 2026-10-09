package com.panomc.plugins.comments.routes.api

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.api.config.PluginConfigManager
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.config.CommentsConfig
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import com.panomc.platform.schema.EndpointDoc
import io.vertx.json.schema.common.dsl.Schemas.*

@Endpoint
class GetConfigAPI(
    private val plugin: CommentsPlugin
) : Api() {
    override val paths = listOf(Path("/comment/config", RouteType.GET))

    override val doc = EndpointDoc(
        summary = "The comment settings the site needs: replies, likes, paging type and who may write.",
        tag = "comments",
        response = objectSchema()
            .requiredProperty("repliesEnabled", booleanSchema())
            .requiredProperty("likesEnabled", booleanSchema())
            .requiredProperty("paginationType", stringSchema())
            .requiredProperty("commentPermission", stringSchema())
            .requiredProperty("replyPermission", stringSchema())
            .requiredProperty("cooldown", intSchema())
            .requiredProperty("inputStyle", stringSchema())
    )

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
