<script>
    import { _ } from "../../../main.js";
    import { Date } from "@panomc/sdk/components";
    import { base } from "@panomc/sdk/svelte";
    import ApiUtil, { buildQueryParams } from "@panomc/sdk/utils/api";

    export let comment;
    export let onDelete;
    export let isReply = false;

    // Use comment.replies if it exists, otherwise empty (for panel where we fetch on demand)
    $: replies = comment.replies || [];
    let showReplies = false;
    let loadingReplies = false;
    let repliesHasMore = false;

    async function fetchReplies() {
        if (loadingReplies) return;
        loadingReplies = true;
        try {
            const lastId = replies.at(-1)?.id;
            const queryParams = buildQueryParams({ lastId, limit: 10 });
            const res = await ApiUtil.get({
                path: `/api/panel/comments/${comment.id}/replies${queryParams}`,
            });
            const next = res.replies || [];
            comment.replies = [...replies, ...next];
            repliesHasMore = next.length === 10;
        } catch (e) {
            console.error("Failed to fetch replies", e);
        } finally {
            loadingReplies = false;
        }
    }

    async function toggleReplies() {
        if (showReplies) {
            showReplies = false;
            return;
        }

        if (replies.length === 0 && comment.replyCount > 0) {
            await fetchReplies();
        }
        showReplies = true;
    }
</script>

<tr class:root-with-replies={showReplies && replies.length > 0}>
    <td class="align-middle">
        <div class="dropdown position-static">
            <button
                type="button"
                class="btn btn-sm btn-link"
                data-bs-toggle="dropdown"
                title={$_("pages.comments.table.actions")}
            >
                <span class="fas fa-ellipsis-v"></span>
            </button>
            <div
                class="dropdown-menu dropdown-menu-start animate__animated animate__fadeIn"
            >
                <button
                    type="button"
                    class="dropdown-item text-danger"
                    on:click={() => onDelete(comment.id)}
                >
                    <i class="fas fa-trash me-2"></i>
                    {$_("pages.comments.table.delete")}
                </button>
            </div>
        </div>
    </td>
    <td class="align-middle">
        <span class="small">#{comment.id}</span>
    </td>
    <td>
        <a
            href="{base}/players/detail/{comment.username}"
            class="d-flex align-items-center gap-2 text-decoration-none text-reset"
        >
            <img
                src="https://minotar.net/avatar/{comment.username || 'char'}"
                class="rounded-circle"
                width="32"
                height="32"
                alt=""
            />
            <span class="fw-medium">{comment.username}</span>
            {#if comment.userGroup && comment.userGroup.name !== "default"}
                <span
                    class="badge bg-light text-muted fw-normal border"
                    style="font-size: 0.65rem;"
                >
                    {comment.userGroup.displayName}
                </span>
            {/if}
        </a>
    </td>
    <td class="align-middle px-3">
        <div class="d-flex flex-column gap-1">
            <div class="text-wrap" style="max-width: 500px;">
                {comment.content}
            </div>
            <div class="d-flex align-items-center gap-3">
                <div class="small mt-1 opacity-75">
                    <a
                        href="{base}/posts/detail/{comment.postId}"
                        class="text-decoration-none text-muted hover-primary"
                    >
                        <i class="fas fa-link me-1"></i>
                        {$_("pages.comments.table.post-id", {
                            values: { postId: comment.postId },
                        })}
                    </a>
                </div>

                {#if !isReply && comment.replyCount > 0}
                    <button
                        type="button"
                        class="btn btn-link btn-sm p-0 text-decoration-none small mt-1 fw-medium"
                        on:click={toggleReplies}
                        disabled={loadingReplies}
                    >
                        {#if loadingReplies}
                            <span
                                class="spinner-border spinner-border-sm me-1"
                                role="status"
                            ></span>
                        {:else}
                            <i class="fas fa-comments me-1"></i>
                        {/if}
                        {comment.replyCount}
                        {$_("pages.comments.table.replies") || "Yanıt"}
                    </button>
                {/if}
            </div>
        </div>
    </td>
    <td class="align-middle">
        {comment.likes || 0}
    </td>
    <td>
        <Date time={comment.createdAt} />
    </td>
</tr>

{#if showReplies}
    {#each replies as reply, i (reply.id)}
        <tr
            class="reply-row bg-light bg-opacity-10"
            class:last-reply={i === replies.length - 1}
        >
            <td class="align-middle">
                <div class="dropdown position-static">
                    <button
                        type="button"
                        class="btn btn-sm btn-link p-0 ms-1"
                        data-bs-toggle="dropdown"
                        title={$_("pages.comments.table.actions")}
                    >
                        <span class="fas fa-ellipsis-v small opacity-50"></span>
                    </button>
                    <div
                        class="dropdown-menu dropdown-menu-start animate__animated animate__fadeIn"
                    >
                        <button
                            type="button"
                            class="dropdown-item text-danger small"
                            on:click={() => onDelete(reply.id)}
                        >
                            <i class="fas fa-trash me-2"></i>
                            {$_("pages.comments.table.delete")}
                        </button>
                    </div>
                </div>
            </td>
            <td class="align-middle">
                <span class="small opacity-50">#{reply.id}</span>
            </td>
            <td class="position-relative">
                <div class="reply-line"></div>
                <a
                    href="{base}/players/detail/{reply.username}"
                    class="d-flex align-items-center gap-2 text-decoration-none text-reset ms-3"
                >
                    <img
                        src="https://minotar.net/avatar/{reply.username ||
                            'char'}"
                        class="rounded-circle border border-2 border-white shadow-sm"
                        width="24"
                        height="24"
                        alt=""
                    />
                    <span class="fw-medium small">{reply.username}</span>
                    {#if reply.userGroup && reply.userGroup.name !== "default"}
                        <span
                            class="badge bg-light text-muted fw-normal border"
                            style="font-size: 0.6rem;"
                        >
                            {reply.userGroup.displayName}
                        </span>
                    {/if}
                </a>
            </td>
            <td>
                <div class="text-wrap small ps-3" style="max-width: 500px;">
                    {reply.content}
                </div>
            </td>
            <td class="align-middle">
                <div class="small ps-3">
                    {reply.likes || 0}
                </div>
            </td>
            <td class="align-middle">
                <div class="small ps-3">
                    <Date time={reply.createdAt} />
                </div>
            </td>
        </tr>
    {/each}
    {#if repliesHasMore}
        <tr class="reply-row bg-light bg-opacity-10">
            <td colspan="2"></td>
            <td colspan="4" class="py-2">
                <button
                    type="button"
                    class="btn btn-link btn-sm text-decoration-none small ms-3 fw-medium p-0"
                    on:click={fetchReplies}
                    disabled={loadingReplies}
                >
                    {#if loadingReplies}
                        <span
                            class="spinner-border spinner-border-sm me-1"
                            role="status"
                        ></span>
                    {/if}
                    {$_("pages.comments.table.load-more-replies", {
                        values: { count: comment.replyCount - replies.length },
                    })}
                </button>
            </td>
        </tr>
    {/if}
{/if}

<style>
    .reply-row td {
        border-top: none !important;
        border-bottom: none !important;
    }

    .reply-row.last-reply td {
        border-bottom: 1px solid rgba(0, 0, 0, 0.1) !important;
    }

    tr.root-with-replies td {
        border-bottom: none !important;
    }

    .reply-row {
        transition: background-color 0.2s;
    }

    .reply-row td > *:not(.reply-line) {
        animation: fadeInRow 0.2s ease-in-out;
    }

    @keyframes fadeInRow {
        from {
            opacity: 0;
        }
        to {
            opacity: 1;
        }
    }

    .reply-row:hover {
        background-color: rgba(0, 0, 0, 0.02) !important;
    }

    .reply-line {
        position: absolute;
        left: 0;
        top: -10px;
        bottom: 50%;
        width: 15px;
        border-left: 2px solid #dee2e6;
        border-bottom: 2px solid #dee2e6;
        border-bottom-left-radius: 8px;
    }
</style>
