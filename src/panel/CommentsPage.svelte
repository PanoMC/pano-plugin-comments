<CommentTable
  {comments}
  commentCount={data.page.totalItems}
  page={data.page}
  onDelete={deleteComment}
  {onPageClick}
  {highlightedComment} />

<ConfirmDeleteCommentModal />

<script context="module">
    import {buildQueryParams} from '@panomc/sdk/utils/api';
  import { api } from '@panomc/sdk/plugin-api';

    export async function load(event) {
    const {
      parent,
      url: { searchParams },
    } = event;
    const { pageTitle } = await parent();

    pageTitle.set('plugins.pano-plugin-comments.pages.comments.title');

    try {
      // Get all current search parameters
      const params = Object.fromEntries(searchParams.entries());
      // Ensure 'page' parameter is set, defaulting to 1 if not present
      params.page = parseInt(params.page || '1');

      const queryParams = buildQueryParams(params);
      const res = await api.panel.get({
        path: `/comments${queryParams}`,
        request: event,
      });
      return { data: { ...res, comments: res.items || [] } };
    } catch (e) {
      console.error('Failed to load comments', e);
    }

    return {
      data: { comments: [], page: { number: 1, size: 10, totalItems: 0, totalPages: 0 } },
    };
  }
</script>

<script>
  import { onMount } from 'svelte';
  import { get } from 'svelte/store';
  import { base, goto, page } from '@panomc/sdk/svelte';
  import CommentTable from './components/view/CommentTable.svelte';
  import ConfirmDeleteCommentModal, {
    show as showDeleteCommentModal,
    setCallback as setDeleteCommentModalCallback,
  } from './components/modals/ConfirmDeleteCommentModal.svelte';

  export let data;

  let comments = [];

  $: comments = data.comments || [];
  $: highlightedComment = data.highlightedComment;

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
      data = {
        ...data,
        comments: newComments,
      };
    } else {
      const oldCount = comments.length;
      const updatedComments = comments.filter((c) => c.id != id);
      if (updatedComments.length < oldCount) {
        comments = updatedComments;
        data = {
          ...data,
          comments: updatedComments,
          page: { ...data.page, totalItems: Math.max(0, data.page.totalItems - 1) },
        };
      }
    }
  });

  async function onPageClick(pageNum) {
    const url = new URL(get(page).url);
    url.searchParams.set('page', pageNum.toString());
    await goto(url.toString(), {
      invalidateAll: true,
    });
  }
</script>
