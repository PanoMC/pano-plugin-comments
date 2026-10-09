<style>
</style>

<div id="pano-comments-section">
  <div class="card mb-3">
    <div class="card-header d-flex justify-content-between align-items-center">
      <div>
        {$_('pages.comments.table-title', {
          values: {
            commentCount: pageInfo.totalItems,
          },
        })}
      </div>
      <div class="form-check form-switch mb-0">
        <input
          checked={commentsEnabled}
          class="form-check-input"
          id="commentsEnabledSwitch"
          on:change={toggleComments}
          type="checkbox" />
        <label class="form-check-label" for="commentsEnabledSwitch">
          {commentsEnabled
            ? $_('settings.enabled')
            : $_('settings.disabled')}
        </label>
      </div>
    </div>
    <div class="card-body p-0">
      {#key refreshKey}
        <CommentTable
          {comments}
          commentCount={pageInfo.totalItems}
          page={pageInfo}
          onDelete={deleteComment}
          {onPageClick}
          hideHeader={true} />
      {/key}
    </div>
  </div>
</div>

<ConfirmDeleteCommentModal />

<script context="module">
  import {buildQueryParams} from '@panomc/sdk/utils/api';
  import { api } from '@panomc/sdk/plugin-api';
  import {_} from '../../../main.js';

  export async function load(event) {
    const {
      url: { searchParams },
      params,
    } = event;
    const page = searchParams.get('commentsPage') || 1;
    const postId = params.postId || params.id;

    if (!postId || postId <= 0) {
      return {
        data: {
          comments: [],
          page: { number: 1, size: 10, totalItems: 0, totalPages: 0 },
          commentsEnabled: true,
        },
      };
    }

    try {
      const queryParams = buildQueryParams({ commentsPage: page });
      const res = await api.panel.get({
        path: `/comments/${postId}${queryParams}`,
        request: event,
      });
      return {
        data: {
          comments: res.items || [],
          page: res.page,
          commentsEnabled: res.commentsEnabled !== false,
        },
      };
    } catch (e) {
      console.error('Failed to load post comments', e);
      return {
        data: {
          comments: [],
          page: { number: 1, size: 10, totalItems: 0, totalPages: 0 },
          commentsEnabled: true,
        },
      };
    }
  }
</script>

<script>
  import { page } from '@panomc/sdk/svelte';
  import { get } from 'svelte/store';
  import CommentTable from './CommentTable.svelte';
  import ConfirmDeleteCommentModal, {
    show as showDeleteCommentModal,
    setCallback as setDeleteCommentModalCallback,
  } from '../modals/ConfirmDeleteCommentModal.svelte';
  import { showSuccessToast, showErrorToast } from '../../../main.js';

  export let data;

  let comments = data.comments || [];
  let commentsEnabled = data.commentsEnabled !== false;
  let pageInfo = data.page || { number: 1, size: 10, totalItems: 0, totalPages: 0 };
  let refreshKey = 0; // To force sub-component refresh if needed

  $: postId = $page.params.postId || $page.params.id;

  let lastPostId;
  // Sync from data prop ONLY when post changes
  $: if (postId !== lastPostId) {
    lastPostId = postId;
    comments = data.comments || [];
    pageInfo = data.page || { number: 1, size: 10, totalItems: 0, totalPages: 0 };
    commentsEnabled = data.commentsEnabled !== false;
    refreshKey++;
  }

  const toggleComments = async () => {
    const oldState = commentsEnabled;
    commentsEnabled = !commentsEnabled;
    try {
      await api.panel.put({
        path: `/comment/settings/${postId}`,
        body: { enabled: commentsEnabled },
      });
      showSuccessToast(
        commentsEnabled
          ? $_('settings.comments-enabled-success')
          : $_('settings.comments-disabled-success'),
      );
    } catch (e) {
      console.error(e);
      commentsEnabled = oldState;
      showErrorToast($_('components.toasts.settings-save-error'));
    }
  };

  const deleteComment = (id) => {
    showDeleteCommentModal(id);
  };

  setDeleteCommentModalCallback((id) => {
    let deletedFromReplies = false;
    const newComments = comments.map((c) => {
      if (c.replies?.some((r) => r.id == id)) {
        deletedFromReplies = true;
        return {
          ...c,
          replies: c.replies.filter((r) => r.id != id),
          replyCount: (c.replyCount || 1) - 1,
        };
      }
      return c;
    });

    if (deletedFromReplies) {
      comments = newComments;
    } else {
      const oldCount = comments.length;
      comments = comments.filter((c) => c.id != id);
      if (comments.length < oldCount) {
        pageInfo = { ...pageInfo, totalItems: Math.max(0, pageInfo.totalItems - 1) };
      }
    }
  });

  async function onPageClick(pageNum) {
    try {
      const params = Object.fromEntries(get(page).url.searchParams.entries());
      params.commentsPage = pageNum;
      const queryParams = buildQueryParams(params);

      const res = await api.panel.get({
        path: `/comments/${postId}${queryParams}`,
      });

      // Update local state directly for smoothness
      comments = res.items || [];
      pageInfo = res.page;
      refreshKey++; // Force CommentTable area to see new page

      // Sync URL without full SvelteKit reload
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
  }
</script>
