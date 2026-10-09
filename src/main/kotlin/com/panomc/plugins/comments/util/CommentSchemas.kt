package com.panomc.plugins.comments.util

import io.vertx.json.schema.common.dsl.ObjectSchemaBuilder
import io.vertx.json.schema.common.dsl.Schemas.arraySchema
import io.vertx.json.schema.common.dsl.Schemas.booleanSchema
import io.vertx.json.schema.common.dsl.Schemas.intSchema
import io.vertx.json.schema.common.dsl.Schemas.objectSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema

/** The response shapes of the public comment endpoints, shared by their `EndpointDoc`s. */
object CommentSchemas {
    /** The page object of every list: `{ number, size, totalItems, totalPages }`. */
    fun page(): ObjectSchemaBuilder = objectSchema()
        .requiredProperty("number", intSchema())
        .requiredProperty("size", intSchema())
        .requiredProperty("totalItems", intSchema())
        .requiredProperty("totalPages", intSchema())

    private fun userGroup(): ObjectSchemaBuilder = objectSchema()
        .requiredProperty("name", stringSchema())
        .requiredProperty("displayName", stringSchema())

    /** One comment; a top-level one in a list also carries its first replies. */
    fun comment(withReplies: Boolean): ObjectSchemaBuilder {
        val shape = objectSchema()
            .requiredProperty("id", intSchema())
            .requiredProperty("postId", intSchema())
            .requiredProperty("userId", intSchema().nullable())
            .requiredProperty("username", stringSchema())
            .requiredProperty("content", stringSchema())
            .requiredProperty("likes", intSchema())
            .requiredProperty("parentId", intSchema().nullable())
            .requiredProperty("createdAt", intSchema())
            .requiredProperty("updatedAt", intSchema())
            .requiredProperty("isLiked", booleanSchema())
            .optionalProperty("replyCount", intSchema())
            .optionalProperty("userGroup", userGroup().nullable())

        if (withReplies) {
            shape.optionalProperty("replies", arraySchema().items(comment(withReplies = false)))
        }

        return shape
    }
}
