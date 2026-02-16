package com.panomc.plugins.comments.db.impl

import com.panomc.platform.annotation.Dao
import com.panomc.plugins.comments.db.dao.CommentLikeDao
import com.panomc.plugins.comments.db.model.CommentLike
import io.vertx.kotlin.coroutines.coAwait
import io.vertx.mysqlclient.MySQLClient
import io.vertx.sqlclient.Row
import io.vertx.sqlclient.RowSet
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.Tuple
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Scope

@Dao
@Lazy
@Scope(value = ConfigurableBeanFactory.SCOPE_SINGLETON)
class CommentLikeDaoImpl : CommentLikeDao() {

    override suspend fun init(sqlClient: SqlClient) {
        sqlClient
            .query(
                """
                            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `commentId` bigint NOT NULL,
                              `userId` bigint NOT NULL,
                              `createdAt` BIGINT(20) NOT NULL,
                              PRIMARY KEY (`id`),
                              UNIQUE INDEX (`commentId`, `userId`),
                              INDEX (`commentId`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comment likes table.';
                        """
            )
            .execute()
            .coAwait()
    }

    override suspend fun add(commentLike: CommentLike, sqlClient: SqlClient): Long {
        val query =
            "INSERT IGNORE INTO `${getTablePrefix() + tableName}` (`commentId`, `userId`, `createdAt`) VALUES (?, ?, ?)"

        val rows: RowSet<Row> = sqlClient
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    commentLike.commentId,
                    commentLike.userId,
                    commentLike.createdAt
                )
            )
            .coAwait()

        return rows.property(MySQLClient.LAST_INSERTED_ID)
    }

    override suspend fun deleteByCommentIdAndUserId(commentId: Long, userId: Long, sqlClient: SqlClient) {
        val query = "DELETE FROM `${getTablePrefix() + tableName}` WHERE `commentId` = ? AND `userId` = ?"
        sqlClient.preparedQuery(query).execute(Tuple.of(commentId, userId)).coAwait()
    }

    override suspend fun deleteByCommentId(commentId: Long, sqlClient: SqlClient) {
        val query = "DELETE FROM `${getTablePrefix() + tableName}` WHERE `commentId` = ?"
        sqlClient.preparedQuery(query).execute(Tuple.of(commentId)).coAwait()
    }

    override suspend fun getByCommentIdAndUserId(commentId: Long, userId: Long, sqlClient: SqlClient): CommentLike? {
        val query = "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `commentId` = ? AND `userId` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(commentId, userId)).coAwait()
        return rows.toEntities().getOrNull(0)
    }

    override suspend fun countByCommentId(commentId: Long, sqlClient: SqlClient): Long {
        val query = "SELECT COUNT(*) FROM `${getTablePrefix() + tableName}` WHERE `commentId` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(commentId)).coAwait()
        return rows.first().getLong(0)
    }

    override suspend fun uninstall(sqlClient: SqlClient) {
        sqlClient.query("DROP TABLE IF EXISTS `${getTablePrefix() + tableName}`").execute().coAwait()
    }
}
