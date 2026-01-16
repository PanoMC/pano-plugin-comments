package com.panomc.plugins.comments.db.impl

import com.panomc.platform.annotation.Dao
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.model.Comment
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
class CommentDaoImpl : CommentDao() {

    override suspend fun init(sqlClient: SqlClient) {
        sqlClient
            .query(
                """
                            CREATE TABLE IF NOT EXISTS `${getTablePrefix() + tableName}` (
                              `id` bigint NOT NULL AUTO_INCREMENT,
                              `postId` bigint NOT NULL,
                              `userId` bigint NOT NULL,
                              `content` MEDIUMTEXT NOT NULL,
                              `likes` int NOT NULL DEFAULT 0,
                              `parentId` bigint,
                              `createdAt` BIGINT(20) NOT NULL,
                              `updatedAt` BIGINT(20) NOT NULL,
                              PRIMARY KEY (`id`),
                              INDEX (`postId`)
                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Comments table.';
                        """
            )
            .execute()
            .coAwait()
    }

    override suspend fun add(comment: Comment, sqlClient: SqlClient): Long {
        val query =
            "INSERT INTO `${getTablePrefix() + tableName}` (`postId`, `userId`, `content`, `likes`, `parentId`, `createdAt`, `updatedAt`) VALUES (?, ?, ?, ?, ?, ?, ?)"

        val rows: RowSet<Row> = sqlClient
            .preparedQuery(query)
            .execute(
                Tuple.of(
                    comment.postId,
                    comment.userId,
                    comment.content,
                    comment.likes,
                    comment.parentId,
                    comment.createdAt,
                    comment.updatedAt
                )
            )
            .coAwait()

        return rows.property(MySQLClient.LAST_INSERTED_ID)
    }

    override suspend fun deleteById(id: Long, sqlClient: SqlClient) {
        val query = "DELETE FROM `${getTablePrefix() + tableName}` WHERE `id` = ?"
        sqlClient.preparedQuery(query).execute(Tuple.of(id)).coAwait()
    }

    override suspend fun getById(id: Long, sqlClient: SqlClient): Comment? {
        val query = "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `id` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(id)).coAwait()
        return rows.toEntities().getOrNull(0)
    }

    override suspend fun countByPostId(postId: Long, sqlClient: SqlClient): Long {
        val query = "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` WHERE `postId` = ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId)).coAwait()
        if (rows.size() == 0) return 0L
        return rows.toList()[0].getLong(0)
    }

    override suspend fun getByPostId(postId: Long, page: Long?, limit: Long?, sqlClient: SqlClient): List<Comment> {
        return if (page != null && limit != null) {
            val offset = (page - 1) * limit
            val query =
                "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `postId` = ? ORDER BY `createdAt` DESC LIMIT ? OFFSET ?"
            val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId, limit, offset)).coAwait()
            rows.toEntities()
        } else {
            val query =
                "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `postId` = ? ORDER BY `createdAt` DESC"
            val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId)).coAwait()
            rows.toEntities()
        }
    }

    override suspend fun countRootByPostId(postId: Long, sqlClient: SqlClient): Long {
        val query = "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` WHERE `postId` = ? AND `parentId` IS NULL"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId)).coAwait()
        if (rows.size() == 0) return 0L
        return rows.toList()[0].getLong(0)
    }

    override suspend fun getRootByPostId(postId: Long, page: Long?, limit: Long?, sqlClient: SqlClient): List<Comment> {
        return if (page != null && limit != null) {
            val offset = (page - 1) * limit
            val query =
                "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `postId` = ? AND `parentId` IS NULL ORDER BY `createdAt` DESC LIMIT ? OFFSET ?"
            val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId, limit, offset)).coAwait()
            rows.toEntities()
        } else {
            val query =
                "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `postId` = ? AND `parentId` IS NULL ORDER BY `createdAt` DESC"
            val rows = sqlClient.preparedQuery(query).execute(Tuple.of(postId)).coAwait()
            rows.toEntities()
        }
    }

    override suspend fun getAll(page: Long, limit: Long, sqlClient: SqlClient): List<Comment> {
        val offset = (page - 1) * limit
        val query = "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` ORDER BY `createdAt` DESC LIMIT ? OFFSET ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(limit, offset)).coAwait()
        return rows.toEntities()
    }

    override suspend fun count(sqlClient: SqlClient): Long {
        val query = "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}`"
        val rows = sqlClient.query(query).execute().coAwait()
        if (rows.size() == 0) return 0L
        return rows.toList()[0].getLong(0)
    }

    override suspend fun getAllRoot(page: Long, limit: Long, sqlClient: SqlClient): List<Comment> {
        val offset = (page - 1) * limit
        val query = "SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `parentId` IS NULL ORDER BY `createdAt` DESC LIMIT ? OFFSET ?"
        val rows = sqlClient.preparedQuery(query).execute(Tuple.of(limit, offset)).coAwait()
        return rows.toEntities()
    }

    override suspend fun countRoot(sqlClient: SqlClient): Long {
        val query = "SELECT COUNT(`id`) FROM `${getTablePrefix() + tableName}` WHERE `parentId` IS NULL"
        val rows = sqlClient.query(query).execute().coAwait()
        if (rows.size() == 0) return 0L
        return rows.toList()[0].getLong(0)
    }

    override suspend fun getRepliesByParentId(parentId: Long, lastId: Long?, limit: Int?, sqlClient: SqlClient): List<Comment> {
        val query = StringBuilder("SELECT ${fields.toTableQuery()} FROM `${getTablePrefix() + tableName}` WHERE `parentId` = ?")
        val params = mutableListOf<Any>(parentId)

        if (lastId != null) {
            query.append(" AND `id` < ?")
            params.add(lastId)
        }

        query.append(" ORDER BY `createdAt` DESC")

        if (limit != null) {
            query.append(" LIMIT ?")
            params.add(limit)
        }

        val rows = sqlClient.preparedQuery(query.toString()).execute(Tuple.from(params)).coAwait()
        return rows.toEntities()
    }

    override suspend fun getReplyCounts(parentIds: List<Long>, sqlClient: SqlClient): Map<Long, Int> {
        if (parentIds.isEmpty()) return emptyMap()
        val placeholders = parentIds.joinToString(",") { "?" }
        val query = "SELECT parentId, COUNT(*) as count FROM `${getTablePrefix() + tableName}` WHERE parentId IN ($placeholders) GROUP BY parentId"
        val rowSet = sqlClient.preparedQuery(query).execute(Tuple.from(parentIds)).coAwait()
        val counts = mutableMapOf<Long, Int>()
        rowSet.forEach { row ->
            counts[row.getLong("parentId")] = row.getInteger("count")
        }
        return counts
    }

    override suspend fun updateLikes(id: Long, likes: Int, sqlClient: SqlClient) {
        val query = "UPDATE `${getTablePrefix() + tableName}` SET `likes` = ?, `updatedAt` = ? WHERE `id` = ?"
        sqlClient.preparedQuery(query).execute(Tuple.of(likes, System.currentTimeMillis(), id)).coAwait()
    }

    override suspend fun updateContent(id: Long, content: String, sqlClient: SqlClient) {
        val query = "UPDATE `${getTablePrefix() + tableName}` SET `content` = ?, `updatedAt` = ? WHERE `id` = ?"
        sqlClient.preparedQuery(query).execute(Tuple.of(content, System.currentTimeMillis(), id)).coAwait()
    }

    override suspend fun getCommentCounts(postIds: List<Long>, sqlClient: SqlClient): Map<Long, Int> {
        if (postIds.isEmpty()) return emptyMap()

        val placeholders = postIds.joinToString(",") { "?" }
        val query = "SELECT postId, COUNT(*) as count FROM `${getTablePrefix() + tableName}` WHERE postId IN ($placeholders) GROUP BY postId"

        val rowSet = sqlClient.preparedQuery(query).execute(Tuple.from(postIds)).coAwait()
        val counts = mutableMapOf<Long, Int>()
        rowSet.forEach { row ->
            counts[row.getLong("postId")] = row.getInteger("count")
        }
        return counts
    }

    override suspend fun uninstall(sqlClient: SqlClient) {
        sqlClient.query("DROP TABLE IF EXISTS `${getTablePrefix() + tableName}`").execute().coAwait()
    }
}
