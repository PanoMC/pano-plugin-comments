package com.panomc.plugins.comments.event

import com.panomc.platform.api.annotation.EventListener
import com.panomc.platform.api.event.PlayerEventListener
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.db.model.User
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.dao.CommentLikeDao

@EventListener
class PlayerEventHandler(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao,
    private val commentLikeDao: CommentLikeDao
): PlayerEventListener {
    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    override suspend fun onDelete(user: User) {
        val sqlClient = databaseManager.getSqlClient()

        val rootComments = commentDao.getRootCommentsByUserId(user.id, sqlClient)

        rootComments.forEach {
            val subComments = commentDao.getRepliesByParentId(it.id, null, null, sqlClient)

            subComments.forEach {
                commentLikeDao.deleteByCommentId(it.id, sqlClient) // delete sub comment likes
            }

            commentLikeDao.deleteByCommentId(it.id, sqlClient) // delete comment likes
            commentDao.deleteByParentId(it.id, sqlClient) // delete sub comments
        }

        commentDao.deleteByUserId(user.id, sqlClient) // delete comments belongs to user
    }
}