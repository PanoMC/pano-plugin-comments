<style>
  .replies-container {
    position: relative;
    margin-left: 1.25rem;
  }

  @media (min-width: 576px) {
    .replies-container {
      margin-left: 3rem;
    }
  }

  .comment-wrapper {
    position: relative;
  }

  .reply-line {
    position: absolute;
    left: -0.75rem;
    top: -1rem;
    bottom: 0;
    width: 0.75rem;
    opacity: 0.5;
    z-index: 0;
    background-image: linear-gradient(
      to bottom,
      var(--bs-border-color) 2.25rem,
      transparent 2.25rem
    );
    background-size: 2px 100%;
    background-repeat: no-repeat;
  }

  @media (min-width: 576px) {
    .reply-line {
      left: -1.5rem;
      width: 1.5rem;
    }
  }

  .reply-line:not(.is-last) {
    background-image:
      linear-gradient(to bottom, var(--bs-border-color) 2.25rem, transparent 2.25rem),
      linear-gradient(to bottom, transparent 3.75rem, var(--bs-border-color) 3.75rem);
  }

  .reply-line::after {
    content: '';
    position: absolute;
    top: 2.25rem;
    left: 0;
    width: 0.75rem;
    height: 1.5rem;
    border-bottom: 2px solid var(--bs-border-color);
    border-left: 2px solid var(--bs-border-color);
    border-bottom-left-radius: 12px;
    background: transparent;
  }

  @media (min-width: 576px) {
    .reply-line::after {
      width: 1.5rem;
    }
  }
</style>

<div class="comment-wrapper" class:ms-sm-3={isReply} class:ms-2={isReply}>
  {#if isReply}
    <!-- Reply Line Connector -->
    <div class="reply-line" class:is-last={isLast}></div>
  {/if}

  <div class="animate__animated animate__fadeInUp">
    <div class="d-flex gap-3">
      <div class="flex-shrink-0">
        <a href="/player/{comment.username}" class="focus-ring rounded-circle">
          <img
            src="https://minotar.net/avatar/{comment.username}"
            class="rounded-circle"
            width="32"
            height="32"
            alt={comment.username} />
        </a>
      </div>
      <div class="flex-grow-1">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <a href="/player/{comment.username}">{comment.username}</a>
            {#if comment.userGroup && comment.userGroup.name !== 'default'}
              <span class="badge ms-1">
                {comment.userGroup.displayName}
              </span>
            {/if}
          </div>
          <div class="d-flex align-items-center gap-2">
            <div class="d-none d-sm-block small">
              <Date time={comment.createdAt} />
            </div>
            <div class="d-block d-sm-none">
              <Date time={comment.createdAt}>
                <i class="fas fa-calendar-alt"></i>
              </Date>
            </div>
            {#if session?.user?.username === comment.username}
              <button
                class="btn btn-link ms-2 pe-0"
                type="button"
                on:click={() => deleteComment(comment.id)}
                title={$_('pages.comments.table.delete')}>
                <i class="fas fa-trash-alt"></i>
              </button>
            {/if}
          </div>
        </div>
        <p class="mb-0">
          {comment.content}
        </p>

        <div class="d-flex gap-3 align-items-center my-2">
          {#if !isReply && session?.user && repliesEnabled && canReply}
            <button
              title={$_('components.comments.reply')}
              aria-label={$_('components.comments.reply')}
              class="btn btn-link"
              type="button"
              on:click={() => toggleReply(comment)}>
              <i class="fas fa-reply"></i>
            </button>
          {/if}
          {#if likesEnabled}
            <button
              class="btn btn-link text-decoration-none ps-0"
              class:link-danger={comment.isLiked}
              type="button"
              on:click={() => toggleLike(comment)}
              disabled={!session?.user}>
              <i class="{comment.isLiked ? 'fas' : 'far'} fa-heart me-1"></i>
              {comment.likes > 0 ? comment.likes : ''}
            </button>
          {/if}
        </div>

        {#if comment.showReplyBox}
          <div class="my-2 animate__animated animate__fadeIn">
            <div class="d-flex gap-2">
              <textarea
                class="form-control"
                rows="2"
                placeholder={$_('components.comments.reply-placeholder')}
                style="resize: none;"
                bind:value={comment.replyContent}></textarea>
              <button
                class="btn btn-secondary align-self-end"
                type="button"
                on:click={() => addComment(comment.id, comment.replyContent)}
                disabled={!comment.replyContent?.trim()}>{$_('components.comments.send')}</button>
            </div>
          </div>
        {/if}
      </div>
    </div>
  </div>

  <div class="replies-container py-2" class:d-none={comment.replies?.length === 0}>
    {#if comment.replies}
      {#each comment.replies as reply, i (reply.id)}
        <svelte:self
          comment={reply}
          isReply={true}
          isLast={i === comment.replies.length - 1}
          {session}
          {deleteComment}
          {toggleReply}
          {toggleLike}
          {addComment}
          {fetchReplies}
          {likesEnabled}
          {repliesEnabled}
          {canReply} />
      {/each}
    {/if}

    {#if !isReply && (comment.repliesHasMore !== undefined ? comment.repliesHasMore : comment.replyCount > (comment.replies?.length || 0))}
      <div class="ms-5 ps-3 mt-2">
        <button
          class="btn btn-secondary btn-sm"
          on:click={() => fetchReplies(comment)}
          disabled={comment.repliesLoading}>
          {#if comment.repliesLoading}
            <span class="spinner-border spinner-border-sm me-1" role="status"></span>
          {/if}
          {$_('components.comments.load-more-replies', {
            values: {
              count: comment.replyCount - (comment.replies?.length || 0),
            },
          })}
        </button>
      </div>
    {/if}
  </div>
</div>

<script>
    import {_} from '../../../main.js';
    import {Date} from '@panomc/sdk/components';

    export let comment;
  export let isReply = false;
  export let isLast = false;
  export let session;
  export let deleteComment;
  export let toggleReply;
  export let toggleLike;
  export let addComment;
  export let fetchReplies;
  export let likesEnabled = true;
  export let repliesEnabled = true;
  export let canReply = true;

  // We use <svelte:self> for recursion
</script>
