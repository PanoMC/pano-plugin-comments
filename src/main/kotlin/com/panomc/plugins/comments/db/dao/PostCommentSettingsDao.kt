package com.panomc.plugins.comments.db.dao

import com.panomc.platform.db.Dao
import com.panomc.plugins.comments.db.model.PostCommentSettings
import io.vertx.sqlclient.SqlClient

abstract class PostCommentSettingsDao : Dao<PostCommentSettings>(PostCommentSettings::class.java) {
    abstract suspend fun set(postId: Long, enabled: Boolean, sqlClient: SqlClient)
    abstract suspend fun get(postId: Long, sqlClient: SqlClient): PostCommentSettings?
}
