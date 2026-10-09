<div id="pano-player-comments-section">
  {#key refreshKey}
    <CommentTable
      {comments}
      commentCount={pageInfo.totalItems}
      page={pageInfo}
      onDelete={deleteComment}
      {onPageClick} />
  {/key}
</div>

<ConfirmDeleteCommentModal />

<script context="module">
  import {buildQueryParams} from '@panomc/sdk/utils/api';
  import { api } from '@panomc/sdk/plugin-api';

  export async function load(event) {
    const {
      url: { searchParams },
      params,
    } = event;
    const page = searchParams.get('commentsPage') || 1;
    const username = params.username;

    if (!username) {
      return {
        data: {
          comments: [],
          page: { number: 1, size: 10, totalItems: 0, totalPages: 0 },
        },
      };
    }

    try {
      const queryParams = buildQueryParams({ page });
      const res = await api.panel.get({
        path: `/comments/player/${username}${queryParams}`,
        request: event,
      });
      return {
        data: {
          comments: res.items || [],
          page: res.page,
        },
      };
    } catch (e) {
      console.error('Failed to load player comments', e);
      return {
        data: {
          comments: [],
          page: { number: 1, size: 10, totalItems: 0, totalPages: 0 },
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

  export let data;

  $: username = $page.params.username;

  let comments = [];
  let pageInfo = { number: 1, size: 10, totalItems: 0, totalPages: 0 };

  let lastUsername;
  $: if (username !== lastUsername) {
    lastUsername = username;
    comments = data?.comments ?? [];
    pageInfo = data?.page ?? { number: 1, size: 10, totalItems: 0, totalPages: 0 };
    refreshKey++;
  }

  let refreshKey = 0;

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
          replyCount: Math.max(0, (c.replyCount || 1) - 1),
        };
      }
      return c;
    });

    if (deletedFromReplies) {
      comments = newComments;
    } else {
      const oldCount = comments.length;
      const filtered = comments.filter((c) => c.id != id);
      if (filtered.length < oldCount) {
        comments = filtered;
        pageInfo = { ...pageInfo, totalItems: Math.max(0, pageInfo.totalItems - 1) };
      }
    }
  });

  async function onPageClick(pageNum) {
    try {
      const params = Object.fromEntries(get(page).url.searchParams.entries());
      params.page = pageNum;
      const queryParams = buildQueryParams(params);

      const res = await api.panel.get({
        path: `/comments/player/${username}${queryParams}`,
      });

      comments = res.items || [];
      pageInfo = res.page;
      refreshKey++;

      const url = new URL(window.location.href);
      url.searchParams.set('commentsPage', pageNum.toString());
      window.history.pushState({}, '', url.toString());

      const element = document.getElementById('pano-player-comments-section');
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
      }
    } catch (e) {
      console.error('Failed to refresh player comments', e);
    }
  }
</script>
