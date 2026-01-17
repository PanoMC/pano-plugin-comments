package com.panomc.plugins.comments.db.impl

import com.panomc.platform.annotation.Dao
import com.panomc.plugins.comments.db.dao.PostCommentSettingsDao
import com.panomc.plugins.comments.db.model.PostCommentSettings
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope

@Dao
@Lazy
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class PostCommentSettingsDaoImpl : PostCommentSettingsDao() {

    override suspend fun init(sqlClient: SqlClient) {
        sqlClient.query(
            """
            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                `id` bigint NOT NULL AUTO_INCREMENT,
                `postId` bigint NOT NULL,
                `enabled` tinyint(1) NOT NULL DEFAULT 1,
                PRIMARY KEY (`id`),
                UNIQUE KEY `unique_post` (`postId`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
        """
        ).execute().coAwait()
    }

    override suspend fun set(postId: Long, enabled: Boolean, sqlClient: SqlClient) {
        val query = """
            INSERT INTO `${getTablePrefix() + tableName}` (postId, enabled)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE enabled = VALUES(enabled)
        """.trimIndent()
        sqlClient.preparedQuery(query).execute(Tuple.of(postId, enabled)).coAwait()
    }

    override suspend fun get(postId: Long, sqlClient: SqlClient): PostCommentSettings? {
        val query = "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `postId` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId)).coAwait()
        return rows.toEntities().getOrNull(0)
    }

    override suspend fun uninstall(sqlClient: SqlClient) {
        sqlClient.query("DROP TABLE IF EXISTS `${getTablePrefix() + tableName}`").execute().coAwait()
    }
}
