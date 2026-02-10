<style>
  .pano-comments-container {
    width: 100%;
  }

  @media (min-width: 992px) {
    .pano-comments-container {
      width: 50%;
    }
  }
</style>

<div
  class="vstack gap-3 animate__animated animate__fadeIn pano-comments-container mx-auto"
  id="pano-comments-section">
  <div class="text-center">
    <h4 class="mb-0">
      {totalComments > 0
        ? $_('components.comments.count', {
            values: { count: totalComments },
          })
        : $_('pages.comments.title')}
    </h4>
  </div>

  <!-- New Comment Trigger -->
  {#if !commentsEnabled}
    <div class="alert alert-secondary text-center">
      {$_('components.comments.closed')}
    </div>
  {:else if session?.user}
    {#if canComment}
      {#if inputStyle === 'INLINE'}
        <div class="mb-4">
          <textarea
            class="form-control mb-2"
            rows="3"
            autocomplete="off"
            placeholder={$_('components.comments.placeholder')}
            bind:value={newCommentContent}></textarea>
          <div class="d-flex justify-content-end">
            <button
              class="btn btn-primary"
              on:click={() => {
                addComment(null, newCommentContent);
                newCommentContent = '';
              }}
              disabled={!newCommentContent.trim()}>
              {$_('components.comments.send')}
            </button>
          </div>
        </div>
      {:else}
        <div class="d-flex justify-content-center">
          <button class="btn btn-link text-decoration-none" on:click={showAddModal}>
            <i class="fas fa-plus me-2"></i>
            {$_('components.comments.add-comment')}
          </button>
        </div>
      {/if}
    {:else}
      <div class="alert alert-warning text-center">
        {$_('components.comments.no-permission')}
      </div>
    {/if}
  {:else}
    <div class="alert alert-info text-center">
      {$_('components.comments.login-required')}
    </div>
  {/if}

  <!-- Comments Area with Local Re-creation -->
  {#key refreshKey}
    <div class="list-group">
      {#if comments.length === 0}
        <NoContent text={$_('components.comments.no-comments')} />
      {:else}
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
              {likesEnabled}
              {repliesEnabled}
              {canReply}
              {commentsEnabled} />
          </div>
        {/each}
      {/if}
    </div>

    <!-- Pagination or Load More -->
    {#if totalComments > 0}
      {#if isLoadMore}
        {#if currentPage < totalPage}
          <div class="d-flex justify-content-center mt-3">
            <button class="btn btn-primary" on:click={loadMore} disabled={loadingMore}>
              {#if loadingMore}
                <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"
                ></span>
              {/if}
              {$_('components.comments.load-more')}
            </button>
          </div>
        {/if}
      {:else}
        <div class="d-flex justify-content-center">
          <Pagination
            page={currentPage}
            {totalPage}
            on:firstPageClick={() => onPageClick(1)}
            on:lastPageClick={() => onPageClick(totalPage)}
            on:pageLinkClick={(event) => onPageClick(event.detail.page)} />
        </div>
      {/if}
    {/if}
  {/key}
</div>

<ConfirmDeleteCommentModal />
<AddCommentModal />

<script>
  import {page} from '@panomc/sdk/svelte';
  import {get} from 'svelte/store';
  import {_} from '../../../main.js';
  import {NoContent, Pagination} from '@panomc/sdk/components/theme';
  import ApiUtil, {buildQueryParams} from '@panomc/sdk/utils/api';
  import {hasPermission} from '@panomc/sdk/utils/auth';
  import ConfirmDeleteCommentModal, {
    setCallback as setDeleteCallback,
    show as showDeleteModal,
  } from '../modals/ConfirmDeleteCommentModal.svelte';
  import {showToast} from '@panomc/sdk/toasts';
  import ThemeCommentItem from './ThemeCommentItem.svelte';
  import AddCommentModal, {
    setCallback as setAddCallback,
    show as showAddModal,
  } from '../modals/AddCommentModal.svelte';

  // Props
  export let post;

  // State
  $: session = $page.data.session;
  let currentPage = parseInt(post.commentData.page) || 1;

  let comments = post.commentData?.comments || [];
  let totalComments = post.commentData?.totalComments || 0;
  $: config = post.commentData?.config || {};
  $: likesEnabled = config.likesEnabled !== false;
  $: repliesEnabled = config.repliesEnabled !== false;
  $: isLoadMore = config.paginationType === 'LOAD_MORE';

  $: commentPermission = config.commentPermission || '';
  $: replyPermission = config.replyPermission || '';
  $: inputStyle = config.inputStyle || 'MODAL'; // "MODAL" or "INLINE"
  $: commentsEnabled = post.commentData?.commentsEnabled !== false;

  $: canComment = hasPermission(commentPermission);
  $: canReply = hasPermission(replyPermission);

  $: totalPage = Math.ceil((totalComments || 0) / 10) || 1;
  let refreshKey = 0; // To force sub-component refresh if needed
  let loadingMore = false;

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
        refreshKey++;
      } else {
        const oldCount = comments.length;
        comments = comments.filter((c) => c.id !== id);
        if (comments.length < oldCount) {
          totalComments--;
          refreshKey++;
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
      url.searchParams.set('commentsPage', pageNum.toString());
      window.history.pushState({}, '', url.toString());

      // Scroll to the section
      const element = document.getElementById('pano-comments-section');
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
      }
    } catch (e) {
      console.error('Failed to refresh comments', e);
    }
  };

  const loadMore = async () => {
    if (loadingMore) return;
    loadingMore = true;
    try {
      const nextPage = currentPage + 1;
      const params = Object.fromEntries(get(page).url.searchParams.entries());
      params.page = nextPage;
      const queryParams = buildQueryParams(params);

      const res = await ApiUtil.get({
        path: `/api/comments/${post.id}${queryParams}`,
      });

      const newComments = res.comments || [];
      comments = [...comments, ...newComments];
      totalComments = res.totalComments || 0;
      currentPage = nextPage;

      // Update URL without refresh
      const url = new URL(window.location.href);
      url.searchParams.set('commentsPage', nextPage.toString());
      window.history.pushState({}, '', url.toString());
    } catch (e) {
      console.error('Failed to load more comments', e);
    } finally {
      loadingMore = false;
    }
  };

  let newCommentContent = '';

  const addComment = async (parentId = null, content = '') => {
    const text = content.trim();
    if (!text) return;

    try {
      const body = { postId: post.id, content: text };
      if (parentId) body.parentId = parentId;

      const res = await ApiUtil.post({
        path: '/api/comments',
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
            p.replyContent = '';
            p.replies = [newComment, ...(p.replies || [])];
            p.repliesHasMore = p.replyCount > p.replies.length;
            comments = [...comments]; // Trigger update
          }
        } else {
          comments = [newComment, ...comments];
          totalComments++;
          refreshKey++;
        }
        showToast($_('toasts.add-success'));
      }

      if (res.error === 'RATE_LIMITED') {
        showToast($_('toasts.rate-limited'));
      }
    } catch (e) {
      console.error('Failed to add comment', e);
      showToast($_('toasts.add-error'));
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
        path: '/api/comments/like/toggle',
        body: { commentId: comment.id },
      });
      const data = res.data || res;
      if (data.likes !== undefined) comment.likes = data.likes;
      if (data.isLiked !== undefined) comment.isLiked = data.isLiked;
      comments = [...comments]; // Trigger update
    } catch (e) {
      console.error('Failed to toggle like', e);
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
      console.error('Failed to fetch replies', e);
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
