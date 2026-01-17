<script>
  import { page } from "@panomc/sdk/svelte";
  import { get } from "svelte/store";
  import { _ } from "../../../main.js";
  import { Pagination, NoContent } from "@panomc/sdk/components";
  import ApiUtil, { buildQueryParams } from "@panomc/sdk/utils/api";
  import ConfirmDeleteCommentModal, {
    setCallback as setDeleteCallback,
    show as showDeleteModal,
  } from "../modals/ConfirmDeleteCommentModal.svelte";
  import { showToast } from "@panomc/sdk/toasts";
  import ThemeCommentItem from "./ThemeCommentItem.svelte";
  import AddCommentModal, {
    show as showAddModal,
    setCallback as setAddCallback,
  } from "../modals/AddCommentModal.svelte";

  // Props
  export let post;

  // State
  $: session = $page.data.session;
  let currentPage = parseInt(post.commentData.page) || 1;

  let comments = post.commentData?.comments || [];
  let totalComments = post.commentData?.totalComments || 0;
  $: totalPage = Math.ceil((totalComments || 0) / 10) || 1;
  let refreshKey = 0; // To force sub-component refresh if needed

  $: {
    setAddCallback(async (content) => {
      await addComment(null, content);
    });
  }

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
      const params = Object.fromEntries(get(page).url.searchParams.entries());
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
    const text = content.trim();
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
            p.replies = [newComment, ...(p.replies || [])];
            p.repliesHasMore = p.replyCount > p.replies.length;
            comments = [...comments]; // Trigger update
          }
        } else {
          comments = [newComment, ...comments];
          totalComments++;
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
  class="vstack gap-3 animate__animated animate__fadeIn w-50 mx-auto"
>
  <div class="text-center">
    <h4 class="mb-0">
      {totalComments > 0
        ? $_("components.comments.count", {
            values: { count: totalComments },
          })
        : $_("pages.comments.title")}
    </h4>
  </div>

  <!-- New Comment Modal Trigger -->
  {#if session?.user}
    <div class="d-flex justify-content-center">
      <button class="btn btn-primary" on:click={showAddModal}>
        <i class="fas fa-plus me-2"></i>
        {$_("components.comments.send")}
      </button>
    </div>
  {:else}
    <div class="alert alert-info text-center">
      {$_("components.comments.login-required")}
    </div>
  {/if}

  <!-- Comments Area with Local Re-creation -->
  {#key refreshKey}
    <div class="list-group">
      {#each comments as comment (comment.id)}
        <div class="list-group-item">
          <ThemeCommentItem
            {comment}
            {session}
            {deleteComment}
            {toggleReply}
            {toggleLike}
            {addComment}
            {fetchReplies}
          />
        </div>
      {:else}
        <NoContent text={$_("components.comments.no-comments")} />
      {/each}
    </div>

    <!-- Pagination -->
    {#if totalComments > 0}
      <div class="d-flex justify-content-center">
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
<AddCommentModal />

<style>
</style>
