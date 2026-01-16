package com.panomc.plugins.comments.db.model

import com.panomc.platform.db.DBEntity

open class CommentLike(
    val id: Long = -1,
    val commentId: Long = -1,
    val userId: Long = -1,
    val createdAt: Long = System.currentTimeMillis()
) : DBEntity()
