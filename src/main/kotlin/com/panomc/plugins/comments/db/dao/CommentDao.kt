package com.panomc.plugins.comments.db.dao

import com.panomc.platform.db.Dao
import com.panomc.plugins.comments.db.model.Comment
import io.vertx.sqlclient.SqlClient

abstract class CommentDao : Dao<Comment>(Comment::class.java) {
    abstract suspend fun add(comment: Comment, sqlClient: SqlClient): Long
    abstract suspend fun deleteById(id: Long, sqlClient: SqlClient)
    abstract suspend fun getById(id: Long, sqlClient: SqlClient): Comment?
    abstract suspend fun countByPostId(postId: Long, sqlClient: SqlClient): Long
    abstract suspend fun getByPostId(postId: Long, page: Long? = null, limit: Long? = null, sqlClient: SqlClient): List<Comment>
    abstract suspend fun countRootByPostId(postId: Long, sqlClient: SqlClient): Long
    abstract suspend fun getRootByPostId(postId: Long, page: Long? = null, limit: Long? = null, sqlClient: SqlClient): List<Comment>
    abstract suspend fun getAll(page: Long, limit: Long = 10, sqlClient: SqlClient): List<Comment>
    abstract suspend fun count(sqlClient: SqlClient): Long
    abstract suspend fun getAllRoot(page: Long, limit: Long = 10, sqlClient: SqlClient): List<Comment>
    abstract suspend fun countRoot(sqlClient: SqlClient): Long
    abstract suspend fun getRepliesByParentId(parentId: Long, lastId: Long? = null, limit: Int? = null, sqlClient: SqlClient): List<Comment>
    abstract suspend fun getReplyCounts(parentIds: List<Long>, sqlClient: SqlClient): Map<Long, Int>
    abstract suspend fun updateLikes(id: Long, likes: Int, sqlClient: SqlClient)
    abstract suspend fun updateContent(id: Long, content: String, sqlClient: SqlClient)
    abstract suspend fun getCommentCounts(postIds: List<Long>, sqlClient: SqlClient): Map<Long, Int>
    abstract suspend fun getByUserId(userId: Long, page: Long, limit: Long = 10, sqlClient: SqlClient): List<Comment>
    abstract suspend fun countByUserId(userId: Long, sqlClient: SqlClient): Long
    abstract suspend fun getParticipatedRootByUserId(
        userId: Long,
        page: Long,
        limit: Long = 10,
        sqlClient: SqlClient
    ): List<Comment>

    abstract suspend fun countParticipatedRootByUserId(userId: Long, sqlClient: SqlClient): Long
}
