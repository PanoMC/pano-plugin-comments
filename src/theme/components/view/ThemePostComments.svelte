<script>
    import { page } from "@panomc/sdk/svelte";
    import { get } from "svelte/store";
    import { _ } from "../../../main.js";
    import { Pagination } from "@panomc/sdk/components";
    import ApiUtil, { buildQueryParams } from "@panomc/sdk/utils/api";
    import ConfirmDeleteCommentModal from "../modals/ConfirmDeleteCommentModal.svelte";
    import {
        show as showDeleteModal,
        setCallback as setDeleteCallback,
    } from "../modals/ConfirmDeleteCommentModal.svelte";
    import { showToast } from "@panomc/sdk/toasts";
    import ThemeCommentItem from "./ThemeCommentItem.svelte";

    // Props
    export let post;

    // State
    $: session = $page.data.session;
    let currentPage = parseInt(post.commentData.page) || 1;

    let comments = post.commentData?.comments || [];
    let totalComments = post.commentData?.totalComments || 0;
    $: totalPage = Math.ceil((totalComments || 0) / 10) || 1;
    let newCommentContent = "";
    let refreshKey = 0; // To force sub-component refresh if needed

    $: {
        setDeleteCallback((id) => {
            let deletedFromReplies = false;
            const newComments = comments.map((c) => {
                if (c.replies?.some((r) => r.id === id)) {
                    deletedFromReplies = true;
                    return {
                        ...c,
                        replies: c.replies.filter((r) => r.id !== id),
                        replyCount: (c.replyCount || 1) - 1,
                    };
                }
                return c;
            });

            if (deletedFromReplies) {
                comments = newComments;
                totalComments--;
            } else {
                const oldCount = comments.length;
                comments = comments.filter((c) => c.id !== id);
                if (comments.length < oldCount) {
                    totalComments--;
                }
            }
        });
    }

    let lastPostId;
    // Sync state ONLY when post ID changes (avoid overwriting manual pagination)
    $: if (post.id !== lastPostId) {
        lastPostId = post.id;
        const data = post.commentData;
        if (data) {
            comments = data.comments || [];
            totalComments = data.totalComments || 0;
            currentPage = parseInt(data.page) || 1;
            refreshKey++;
        }
    }

    const onPageClick = async (pageNum) => {
        try {
            const params = Object.fromEntries(
                get(page).url.searchParams.entries(),
            );
            params.page = pageNum;
            const queryParams = buildQueryParams(params);

            const res = await ApiUtil.get({
                path: `/api/comments/${post.id}${queryParams}`,
            });

            // Local update for smoothness
            comments = res.comments || [];
            totalComments = res.totalComments || 0;
            currentPage = pageNum;
            refreshKey++; // Force refresh of the comments and pagination area

            // Sync URL without refresh
            const url = new URL(window.location.href);
            url.searchParams.set("commentsPage", pageNum.toString());
            window.history.pushState({}, "", url.toString());

            console.log(comments);

            // Scroll to the section
            const element = document.getElementById("pano-comments-section");
            if (element) {
                element.scrollIntoView({ behavior: "smooth" });
            }
        } catch (e) {
            console.error("Failed to refresh comments", e);
        }
    };

    const addComment = async (parentId = null, content = "") => {
        const text = (parentId ? content : newCommentContent).trim();
        if (!text) return;

        try {
            const body = { postId: post.id, content: text };
            if (parentId) body.parentId = parentId;

            const res = await ApiUtil.post({
                path: "/api/comments",
                body,
            });

            if (!res.error && res.comment) {
                const newComment = res.comment;
                if (parentId) {
                    const idx = comments.findIndex((c) => c.id === parentId);
                    if (idx !== -1) {
                        const p = comments[idx];
                        p.replyCount = (p.replyCount || 0) + 1;
                        p.showReplyBox = false;
                        p.replyContent = "";
                        p.replies = [...(p.replies || []), newComment];
                        p.repliesHasMore = p.replyCount > p.replies.length;
                        comments = [...comments]; // Trigger update
                    }
                } else {
                    comments = [newComment, ...comments];
                    totalComments++;
                    newCommentContent = "";
                }
                showToast($_("toasts.add-success"));
            }
        } catch (e) {
            console.error("Failed to add comment", e);
        }
    };

    const deleteComment = async (id) => {
        showDeleteModal(id);
    };

    const toggleLike = async (comment) => {
        if (!session?.user) {
            return;
        }

        const old = { likes: comment.likes, isLiked: comment.isLiked };
        comment.isLiked = !old.isLiked;
        comment.likes += old.isLiked ? -1 : 1;
        comments = [...comments]; // Trigger update

        try {
            const res = await ApiUtil.post({
                path: "/api/comments/like/toggle",
                body: { commentId: comment.id },
            });
            const data = res.data || res;
            if (data.likes !== undefined) comment.likes = data.likes;
            if (data.isLiked !== undefined) comment.isLiked = data.isLiked;
            comments = [...comments]; // Trigger update
        } catch (e) {
            console.error("Failed to toggle like", e);
            Object.assign(comment, old);
            comments = [...comments]; // Trigger update
        }
    };

    const fetchReplies = async (c) => {
        if (c.repliesLoading || c.repliesHasMore === false) return;

        c.repliesLoading = true;
        comments = [...comments]; // Trigger update
        try {
            const lastId = c.replies?.at(-1)?.id;
            const queryParams = buildQueryParams({ lastId });
            const res = await ApiUtil.get({
                path: `/api/comments/replies/${c.id}${queryParams}`,
            });
            const next = res.replies || [];
            c.replies = [...(c.replies || []), ...next];
            c.repliesHasMore = next.length === 3;
        } catch (e) {
            console.error("Failed to fetch replies", e);
        } finally {
            c.repliesLoading = false;
            comments = [...comments]; // Trigger update
        }
    };

    const toggleReply = (c) => {
        c.showReplyBox = !c.showReplyBox;
        comments = [...comments]; // Trigger update
    };
</script>

<div
    id="pano-comments-section"
    class="vstack gap-3 mt-5 animate__animated animate__fadeIn"
>
    <div class="d-flex align-items-center justify-content-between px-2">
        <h2 class="mb-0 h5 fw-bold">
            <i class="fas fa-comments me-2 text-primary"></i>
            {totalComments > 0
                ? $_("components.comments.count", {
                      values: { count: totalComments },
                  })
                : $_("pages.comments.title")}
        </h2>
    </div>

    <!-- New Comment Card -->
    {#if session?.user}
        <div class="card rounded-4">
            <div class="card-body p-4">
                <div class="d-flex gap-3">
                    <div class="flex-shrink-0 d-none d-sm-block">
                        <img
                            src="https://minotar.net/avatar/{session?.user
                                ?.username || 'char'}"
                            class="rounded-circle"
                            width="40"
                            height="40"
                            alt="avatar"
                        />
                    </div>
                    <div class="flex-grow-1">
                        <textarea
                            class="form-control bg-body-tertiary p-3 rounded-4 shadow-none"
                            rows="3"
                            placeholder={$_("components.comments.placeholder")}
                            style="resize: none;"
                            bind:value={newCommentContent}
                        ></textarea>
                        <div class="d-flex justify-content-end mt-3">
                            <button
                                class="btn btn-secondary px-4 fw-normal"
                                type="button"
                                on:click={() => addComment()}
                                disabled={!newCommentContent.trim()}
                            >
                                <i class="fas fa-paper-plane me-2 small"></i>
                                {$_("components.comments.send")}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    {:else}
        <div class="alert alert-info rounded-4 border-0">
            {$_("components.comments.login-required")}
        </div>
    {/if}

    <!-- Comments Area with Local Re-creation -->
    {#key refreshKey}
        <div class="vstack gap-3">
            {#each comments as comment (comment.id)}
                <ThemeCommentItem
                    {comment}
                    {session}
                    {deleteComment}
                    {toggleReply}
                    {toggleLike}
                    {addComment}
                    {fetchReplies}
                />
            {:else}
                <div
                    class="card rounded-4 border-0 shadow-sm animate__animated animate__fadeIn mb-3"
                >
                    <div class="card-body p-5 text-center">
                        <div class="mb-3">
                            <i
                                class="fas fa-comments fa-3x text-muted opacity-25"
                            ></i>
                        </div>
                        <p class="text-muted mb-0">
                            {$_("components.comments.no-comments")}
                        </p>
                    </div>
                </div>
            {/each}
        </div>

        <!-- Pagination -->
        {#if totalComments > 0}
            <div class="d-flex justify-content-center mt-2">
                <Pagination
                    page={currentPage}
                    {totalPage}
                    on:firstPageClick={() => onPageClick(1)}
                    on:lastPageClick={() => onPageClick(totalPage)}
                    on:pageLinkClick={(event) => onPageClick(event.detail.page)}
                />
            </div>
        {/if}
    {/key}
</div>

<ConfirmDeleteCommentModal />

<style>
</style>
