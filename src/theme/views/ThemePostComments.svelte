<style>
  .comments-theme-post-comments {
    width: 100%;
  }

  @media (min-width: 992px) {
    .comments-theme-post-comments {
      width: 50%;
    }
  }
</style>

<div class="vstack gap-3 comments-theme-post-comments mx-auto pt-5" id="pano-comments-section">
  <div class="text-center">
    <h4 class="comments-theme-post-comments__title mb-0">
      {totalComments > 0
        ? $_('components.comments.count', {
            values: { count: totalComments },
          })
        : $_('pages.comments.title')}
    </h4>
  </div>

  <!-- New Comment Trigger -->
  {#if !commentsEnabled}
    <div class="comments-theme-post-comments__alert alert alert-secondary text-center">
      {$_('components.comments.closed')}
    </div>
  {:else if session?.user}
    {#if canComment}
      {#if inputStyle === 'INLINE'}
        <div class="mb-4">
          <textarea
            class="comments-theme-post-comments__input form-control mb-2"
            rows="3"
            autocomplete="off"
            placeholder={$_('components.comments.placeholder')}
            bind:value={newCommentContent}></textarea>
          <div class="d-flex justify-content-end">
            <button
              class="comments-theme-post-comments__action btn btn-primary"
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
          <button class="comments-theme-post-comments__add-comment btn btn-primary text-decoration-none" on:click={showAddModal}>
            <i class="fas fa-plus me-2"></i>
            {$_('components.comments.add-comment')}
          </button>
        </div>
      {/if}
    {:else}
      <div class="comments-theme-post-comments__no-permission alert alert-warning text-center">
        {$_('components.comments.no-permission')}
      </div>
    {/if}
  {:else}
    <div class="comments-theme-post-comments__login-required alert alert-info text-center">
      {$_('components.comments.login-required')}
    </div>
  {/if}

  <!-- Comments Area with Local Re-creation -->
  {#key refreshKey}
    <div class="comments-theme-post-comments__list list-group">
      {#if comments.length === 0}
        <NoContent text={$_('components.comments.no-comments')} />
      {:else}
        {#each comments as comment (comment.id)}
          <div class="comments-theme-post-comments__item list-group-item">
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
        {#if currentPage < pageInfo.totalPages}
          <div class="d-flex justify-content-center mt-3">
            <button class="comments-theme-post-comments__load-more btn btn-primary" on:click={loadMore} disabled={loadingMore}>
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
            page={pageInfo}
            on:firstPageClick={() => onPageClick(1)}
            on:lastPageClick={() => onPageClick(pageInfo.totalPages)}
            on:pageLinkClick={(event) => onPageClick(event.detail.page)} />
        </div>
      {/if}
    {/if}
  {/key}
</div>

<ConfirmDeleteCommentModal />
<AddCommentModal />

<script context="module">
  // The comments block under a post: mounts by itself on the post page, or a theme places it with
  // <PluginBlock id="comments:ThemePostComments" post={post} />.
  export const view = { hook: 'theme:post-detail:bottom', block: true };
</script>

<script>
  import { page } from '@panomc/sdk/svelte';
  import { derived, get } from 'svelte/store';
  import { _ as i18n } from '@panomc/sdk/utils/language';
  import { showToast } from '@panomc/sdk/toasts';
  import { NoContent, Pagination } from '@panomc/sdk/components/theme';
  import { buildQueryParams } from '@panomc/sdk/utils/api';
  import { api } from '@panomc/sdk/plugin-api';
  import { hasPermission } from '@panomc/sdk/utils/auth';
  import ConfirmDeleteCommentModal, {
    setCallback as setDeleteCallback,
    show as showDeleteModal,
  } from './ConfirmDeleteCommentModal.svelte';
  import ThemeCommentItem from './ThemeCommentItem.svelte';
  import AddCommentModal, {
    setCallback as setAddCallback,
    show as showAddModal,
  } from './AddCommentModal.svelte';

  // plugin translations: $_('key') reads plugins.pano-plugin-comments.key
  const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-comments.${key}`, options));

  // toasts coloured like the panel's: success / danger variants (an older host ignores the option and renders neutral)
  const showSuccessToast = (text, params = {}) => showToast(text, params, undefined, { variant: 'success' });
  const showErrorToast = (text, params = {}) => showToast(text, params, undefined, { variant: 'danger' });

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

  $: pageInfo = {
    number: currentPage,
    size: 10,
    totalItems: totalComments || 0,
    totalPages: Math.ceil((totalComments || 0) / 10) || 1,
  };
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

      const res = await api.get({
        path: `/comments/${post.id}${queryParams}`,
      });

      // Local update for smoothness
      comments = res.items || [];
      totalComments = res.page?.totalItems || 0;
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

      const res = await api.get({
        path: `/comments/${post.id}${queryParams}`,
      });

      const newComments = res.items || [];
      comments = [...comments, ...newComments];
      totalComments = res.page?.totalItems || 0;
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

      const res = await api.post({
        path: '/comments',
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
        showSuccessToast($_('toasts.add-success'));
      }

      if (res.error?.code === 'RATE_LIMITED') {
        showErrorToast($_('toasts.rate-limited'));
      }
    } catch (e) {
      console.error('Failed to add comment', e);
      showErrorToast($_('toasts.add-error'));
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
      const res = await api.post({
        path: '/comments/like/toggle',
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
      const res = await api.get({
        path: `/comments/replies/${c.id}${queryParams}`,
      });
      const next = res.items || [];
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
