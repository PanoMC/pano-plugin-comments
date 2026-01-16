<script context="module">
    import ApiUtil, { buildQueryParams } from "@panomc/sdk/utils/api";

    export async function load(event) {
        const {
            url: { searchParams },
            params,
        } = event;
        const page = searchParams.get("commentsPage") || 1;
        const postId = params.postId || params.id;

        if (!postId || postId <= 0) {
            return {
                data: {
                    comments: [],
                    totalPage: 1,
                    totalComments: 0,
                    currentPage: 1,
                },
            };
        }

        try {
            const queryParams = buildQueryParams({ commentsPage: page });
            const res = await ApiUtil.get({
                path: `/api/panel/comments/${postId}${queryParams}`,
                request: event,
            });
            return {
                data: {
                    comments: res.comments || [],
                    totalPage: res.totalPage || 1,
                    totalComments: res.totalComments || 0,
                    currentPage: parseInt(page),
                },
            };
        } catch (e) {
            console.error("Failed to load post comments", e);
            return {
                data: {
                    comments: [],
                    totalPage: 1,
                    totalComments: 0,
                    currentPage: 1,
                },
            };
        }
    }
</script>

<script>
    import { page } from "@panomc/sdk/svelte";
    import { get } from "svelte/store";
    import CommentTable from "./CommentTable.svelte";
    import ConfirmDeleteCommentModal, {
        show as showDeleteCommentModal,
        setCallback as setDeleteCommentModalCallback,
    } from "../modals/ConfirmDeleteCommentModal.svelte";

    export let data;

    let comments = data.comments || [];
    let totalPage = data.totalPage || 1;
    let totalComments = data.totalComments || 0;
    let currentPage = data.currentPage || 1;
    let refreshKey = 0; // To force sub-component refresh if needed

    $: postId = $page.params.postId || $page.params.id;

    let lastPostId;
    // Sync from data prop ONLY when post changes
    $: if (postId !== lastPostId) {
        lastPostId = postId;
        comments = data.comments || [];
        totalPage = data.totalPage || 1;
        totalComments = data.totalComments || 0;
        currentPage = data.currentPage || 1;
        refreshKey++;
    }

    const deleteComment = (id) => {
        showDeleteCommentModal(id);
    };

    setDeleteCommentModalCallback((id) => {
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

    async function onPageClick(pageNum) {
        try {
            const params = Object.fromEntries(
                get(page).url.searchParams.entries(),
            );
            params.commentsPage = pageNum;
            const queryParams = buildQueryParams(params);

            const res = await ApiUtil.get({
                path: `/api/panel/comments/${postId}${queryParams}`,
            });

            // Update local state directly for smoothness
            comments = res.comments || [];
            totalPage = res.totalPage || 1;
            totalComments = res.totalComments || 0;
            currentPage = pageNum;
            refreshKey++; // Force CommentTable area to see new page

            // Sync URL without full SvelteKit reload
            const url = new URL(window.location.href);
            url.searchParams.set("commentsPage", pageNum.toString());
            window.history.pushState({}, "", url.toString());

            // Scroll to the section
            const element = document.getElementById("pano-comments-section");
            if (element) {
                element.scrollIntoView({ behavior: "smooth" });
            }
        } catch (e) {
            console.error("Failed to refresh comments", e);
        }
    }
</script>

<div id="pano-comments-section" class="mt-4">
    {#key refreshKey}
        <CommentTable
            {comments}
            commentCount={totalComments}
            page={currentPage}
            {totalPage}
            onDelete={deleteComment}
            {onPageClick}
        />
    {/key}
</div>

<ConfirmDeleteCommentModal />

<style>
</style>
