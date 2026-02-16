package com.panomc.plugins.comments.db.dao

import com.panomc.platform.db.Dao
import com.panomc.plugins.comments.db.model.CommentLike
import io.vertx.sqlclient.SqlClient

abstract class CommentLikeDao : Dao<CommentLike>(CommentLike::class.java) {
    abstract suspend fun add(commentLike: CommentLike, sqlClient: SqlClient): Long
    abstract suspend fun deleteByCommentIdAndUserId(commentId: Long, userId: Long, sqlClient: SqlClient)
    abstract suspend fun deleteByCommentId(commentId: Long, sqlClient: SqlClient)
    abstract suspend fun getByCommentIdAndUserId(commentId: Long, userId: Long, sqlClient: SqlClient): CommentLike?
    abstract suspend fun countByCommentId(commentId: Long, sqlClient: SqlClient): Long
}
