package com.panomc.plugins.comments.db.model

import com.panomc.platform.db.DBEntity

data class PostCommentSettings(
    val id: Long = 0,
    val postId: Long,
    val enabled: Boolean = true
) : DBEntity()
