package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.auth.AuthProvider
import com.panomc.platform.auth.PermissionManager
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.PageNotFound
import com.panomc.platform.model.*
import com.panomc.plugins.comments.CommentsPlugin
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import io.vertx.ext.web.validation.builder.Parameters.optionalParam
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema
import kotlin.math.ceil

@Endpoint
class PanelGetAllCommentsAPI(
    private val plugin: CommentsPlugin,
    private val commentDao: CommentDao
) : PanelApi() {
    override val paths = listOf(Path("/api/panel/comments", RouteType.GET))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(AuthProvider::class.java)
    }

    private val permissionManager by lazy {
        plugin.applicationContext.getBean(PermissionManager::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .queryParameter(optionalParam("page", numberSchema()))
            .queryParameter(optionalParam("commentId", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)

        val parameters = getParameters(context)
        val page = parameters.queryParameter("page")?.long ?: 1L
        val highlightedCommentId = parameters.queryParameter("commentId")?.long

        val limit = 10L
        val sqlClient = databaseManager.getSqlClient()
        val comments = commentDao.getAllRoot(page, limit, sqlClient)
        val count = commentDao.countRoot(sqlClient)

        val totalPageNum = if (count == 0L) 1L else ceil(count.toDouble() / limit).toLong()

        if (totalPageNum in 1..<page) {
            throw PageNotFound()
        }

        suspend fun formatComment(
            comment: com.panomc.plugins.comments.db.model.Comment,
            replyCount: Int,
            username: String?
        ): Map<String, Any?> {
            val map = JsonObject.mapFrom(comment).map
            map["replyCount"] = replyCount
            map["username"] = username ?: "-"
            val userGroup = comment.userId?.let { permissionManager.getPermissionGroup(it) }
            map["userGroup"] = if (userGroup != null) mapOf(
                "name" to userGroup.name,
                "displayName" to userGroup.displayName
            ) else null
            return map
        }

        val highlightedComment = if (highlightedCommentId != null) {
            var targetRoot = commentDao.getById(highlightedCommentId, sqlClient)
            while (targetRoot?.parentId != null) {
                targetRoot = commentDao.getById(targetRoot.parentId!!, sqlClient)
            }

            if (targetRoot != null) {
                val username = targetRoot.userId?.let { databaseManager.userDao.getUsernameFromUserId(it, sqlClient) }
                val replyCount = commentDao.getReplyCounts(listOf(targetRoot.id), sqlClient)[targetRoot.id] ?: 0
                val formattedRoot = formatComment(targetRoot, replyCount, username).toMutableMap()

                // Fetch replies context
                val allReplies = commentDao.getRepliesByParentId(targetRoot.id, sqlClient = sqlClient)
                var replies: List<com.panomc.plugins.comments.db.model.Comment>
                var hiddenAbove = 0
                var hiddenBelow = 0

                if (highlightedCommentId != targetRoot.id) {
                    // Specific reply targeted: show context (3 before, 3 after)
                    val idx = allReplies.indexOfFirst { it.id == highlightedCommentId }
                    if (idx != -1) {
                        val start = maxOf(0, idx - 3)
                        val end = minOf(allReplies.size, idx + 4)
                        replies = allReplies.subList(start, end)
                        hiddenAbove = start
                        hiddenBelow = allReplies.size - end
                    } else {
                        replies = allReplies.take(10)
                    }
                } else {
                    // Root targeted: show absolute first 10 replies (standard pagination)
                    replies = allReplies.take(10)
                }

                val replyUserIds = replies.mapNotNull { it.userId }.toSet().toList()
                val replyUsernames = databaseManager.userDao.getUsernameByListOfId(replyUserIds, sqlClient)

                formattedRoot["replies"] = replies.map { reply ->
                    formatComment(reply, 0, reply.userId?.let { replyUsernames[it] })
                }
                formattedRoot["hiddenAbove"] = hiddenAbove
                formattedRoot["hiddenBelow"] = hiddenBelow
                formattedRoot
            } else null
        } else null

        val resultComments = run {
            val rootIdToExclude = highlightedComment?.get("id") as? Long

            val filteredComments = if (rootIdToExclude != null) {
                comments.filter { it.id != rootIdToExclude }
            } else {
                comments
            }

            val userIds = filteredComments.mapNotNull { it.userId }.toSet().toList()
            val usernames = databaseManager.userDao.getUsernameByListOfId(userIds, sqlClient)
            val replyCounts = commentDao.getReplyCounts(filteredComments.map { it.id }, sqlClient)

            filteredComments.map { comment ->
                formatComment(comment, replyCounts[comment.id] ?: 0, comment.userId?.let { usernames[it] })
            }
        }

        return Successful(
            mapOf(
                "comments" to resultComments,
                "totalPage" to totalPageNum,
                "commentCount" to count,
                "highlightedComment" to highlightedComment
            )
        )
    }
}
