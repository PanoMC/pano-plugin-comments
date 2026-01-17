package com.panomc.plugins.comments.db.migration

import com.panomc.platform.annotation.Migration
import com.panomc.platform.db.DatabaseMigration
import com.panomc.plugins.comments.db.dao.PostCommentSettingsDao
import io.vertx.sqlclient.SqlClient

@Migration
class CommentsMigration1to2(
    private val postCommentSettingsDao: PostCommentSettingsDao
) : DatabaseMigration(1, 2, "Create post comment settings table") {

    override val handlers: List<suspend (SqlClient) -> Unit> = listOf(
        { sqlClient -> postCommentSettingsDao.init(sqlClient) }
    )
}
