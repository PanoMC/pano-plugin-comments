package com.panomc.plugins.comments.config

import com.panomc.platform.api.config.PluginConfig

class CommentsConfig(
    val repliesEnabled: Boolean = true,
    val likesEnabled: Boolean = true,
    val paginationType: PaginationType = PaginationType.PAGINATION,
    val commentPermission: String = "",
    val replyPermission: String = "",
    val cooldown: Int = 5,
    val inputStyle: InputStyle = InputStyle.MODAL,
    version: Int = 1
) : PluginConfig(version)
