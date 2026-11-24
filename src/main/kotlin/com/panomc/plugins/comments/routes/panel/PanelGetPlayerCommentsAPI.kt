package com.panomc.plugins.comments.routes.panel

import com.panomc.platform.annotation.Endpoint
import com.panomc.platform.db.DatabaseManager
import com.panomc.platform.error.PageNotFound
import com.panomc.platform.model.*
import com.panomc.plugins.comments.db.dao.CommentDao
import com.panomc.plugins.comments.db.model.Comment
import com.panomc.plugins.comments.permission.ManageCommentsPermission
import com.panomc.plugins.comments.util.PageInfo
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.validation.ValidationHandler
import com.panomc.platform.schema.dsl.Parameters.optionalParam
import com.panomc.platform.schema.dsl.Parameters.param
import com.panomc.platform.schema.dsl.ValidationHandlerBuilder
import io.vertx.json.schema.SchemaRepository
import io.vertx.json.schema.common.dsl.Schemas.numberSchema
import io.vertx.json.schema.common.dsl.Schemas.stringSchema
import kotlin.math.ceil

@Endpoint
class PanelGetPlayerCommentsAPI(
    private val plugin: com.panomc.plugins.comments.CommentsPlugin,
    private val commentDao: CommentDao
) : PanelApi() {
    override val paths = listOf(Path("/comments/player/:username", RouteType.GET))

    private val databaseManager by lazy {
        plugin.applicationContext.getBean(DatabaseManager::class.java)
    }

    private val authProvider by lazy {
        plugin.applicationContext.getBean(com.panomc.platform.auth.AuthProvider::class.java)
    }

    override fun getValidationHandler(schemaRepository: SchemaRepository): ValidationHandler =
        ValidationHandlerBuilder.create(schemaRepository)
            .pathParameter(param("username", stringSchema()))
            .queryParameter(optionalParam("page", numberSchema()))
            .build()

    override suspend fun handle(context: RoutingContext): Result {
        authProvider.requirePermission(ManageCommentsPermission(), context)
        val parameters = getParameters(context)
        val username = parameters.pathParameter("username").string
        val page = parameters.queryParameter("page")?.long ?: 1L
        val limit = 10L

        val sqlClient = getSqlClient()
        val user = databaseManager.userDao.getByUsername(username, sqlClient) ?: throw PageNotFound()

        val commentCount = commentDao.countParticipatedRootByUserId(user.id, sqlClient)
        val totalPages = ceil(commentCount.toDouble() / limit).toLong().coerceAtLeast(1L)

        if (page > totalPages && commentCount > 0) {
            throw PageNotFound()
        }

        val rootComments = commentDao.getParticipatedRootByUserId(user.id, page, limit, sqlClient)
        if (rootComments.isEmpty()) {
            return Successful(
                mapOf(
                    "items" to emptyList<Map<String, Any?>>(),
                    "page" to PageInfo.of(page, limit.toInt(), 0L)
                )
            )
        }

        val rootIds = rootComments.map { it.id }

        // Fetch all replies by this user for these roots
        val allUserComments = commentDao.getByUserId(user.id, 1, 1000, sqlClient)
            .filter { it.parentId != null && rootIds.contains(it.parentId) || rootIds.contains(it.id) }

        // Fetch usernames for root authors
        val rootUserIds = rootComments.mapNotNull { it.userId }.toSet().toList()
        val usernames = databaseManager.userDao.getUsernameByListOfId(rootUserIds, sqlClient)

        // Fetch total reply counts for roots
        val replyCounts = commentDao.getReplyCounts(rootIds, sqlClient)

        val threads = rootComments.map { root ->
            val thread = formatComment(root, replyCounts[root.id] ?: 0, usernames[root.userId]).toMutableMap()
            val userReplies = allUserComments
                .filter { it.parentId == root.id && it.id != root.id }
                .map { formatComment(it, 0, user.username) }

            thread["replies"] = userReplies
            thread
        }

        return Successful(
            mapOf(
                "items" to threads,
                "page" to PageInfo.of(page, limit.toInt(), commentCount)
            )
        )
    }

    private fun formatComment(comment: Comment, replyCount: Int, username: String?): Map<String, Any?> {
        return mapOf(
            "id" to comment.id,
            "postId" to comment.postId,
            "userId" to comment.userId,
            "username" to username,
            "content" to comment.content,
            "likes" to comment.likes,
            "parentId" to comment.parentId,
            "createdAt" to comment.createdAt,
            "updatedAt" to comment.updatedAt,
            "replyCount" to replyCount,
            "replies" to mutableListOf<Map<String, Any?>>()
        )
    }
}
