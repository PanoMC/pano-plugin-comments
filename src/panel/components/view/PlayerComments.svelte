<script context="module">
    import ApiUtil, {buildQueryParams} from "@panomc/sdk/utils/api";

    export async function load(event) {
        const {
            url: {searchParams},
            params,
        } = event;
        const page = searchParams.get("commentsPage") || 1;
        const username = params.username;

        if (!username) {
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
            const queryParams = buildQueryParams({page});
            const res = await ApiUtil.get({
                path: `/api/panel/comments/player/${username}${queryParams}`,
                request: event,
            });
            return {
                data: {
                    comments: res.comments || [],
                    totalPage: res.totalPage || 1,
                    totalComments: res.commentCount || 0,
                    currentPage: parseInt(page),
                },
            };
        } catch (e) {
            console.error("Failed to load player comments", e);
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
    import {page} from "@panomc/sdk/svelte";
    import {get} from "svelte/store";
    import CommentTable from "./CommentTable.svelte";
    import ConfirmDeleteCommentModal, {
        show as showDeleteCommentModal,
        setCallback as setDeleteCommentModalCallback,
    } from "../modals/ConfirmDeleteCommentModal.svelte";

    export let data;

    $: username = $page.params.username;

    $: comments = data?.comments ?? [];
    $: totalPage = data?.totalPage ?? 1;
    $: totalComments = data?.totalComments ?? 0;
    $: currentPage = data?.currentPage ?? 1;

    let refreshKey = 0;

    let lastUsername;
    $: if (username !== lastUsername) {
        lastUsername = username;
        refreshKey++;
    }

    const deleteComment = (id) => {
        showDeleteCommentModal(id);
    };

    setDeleteCommentModalCallback((id) => {
        const oldCount = comments.length;
        const updated = comments.filter((c) => c.id != id);

        if (updated.length < oldCount) {
            data = {
                ...data,
                comments: updated,
                totalComments: (data.totalComments || 1) - 1,
            };
        }
    });

    async function onPageClick(pageNum) {
        try {
            const params = Object.fromEntries(
                get(page).url.searchParams.entries(),
            );
            params.page = pageNum;
            const queryParams = buildQueryParams(params);

            const res = await ApiUtil.get({
                path: `/api/panel/comments/player/${username}${queryParams}`,
            });

            data = {
                ...data,
                comments: res.comments || [],
                totalPage: res.totalPage || 1,
                totalComments: res.commentCount || 0,
                currentPage: pageNum,
            };
            refreshKey++;

            const url = new URL(window.location.href);
            url.searchParams.set("commentsPage", pageNum.toString());
            window.history.pushState({}, "", url.toString());

            const element = document.getElementById(
                "pano-player-comments-section",
            );
            if (element) {
                element.scrollIntoView({behavior: "smooth"});
            }
        } catch (e) {
            console.error("Failed to refresh player comments", e);
        }
    }
</script>

<div class="mt-3" id="pano-player-comments-section">
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

<ConfirmDeleteCommentModal/>

<style>
</style>
