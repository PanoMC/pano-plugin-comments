package com.panomc.plugins.comments.db.model

import com.panomc.platform.db.DBEntity

open class Comment(
    var id: Long = -1,
    val postId: Long = -1,
    val userId: Long? = null,
    val content: String = "",
    val likes: Int = 0,
    val parentId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
) : DBEntity()
