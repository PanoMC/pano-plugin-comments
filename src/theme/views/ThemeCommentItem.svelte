<style>
  .comments-theme-comment-item__replies {
    position: relative;
    margin-left: 1.25rem;
  }

  @media (min-width: 576px) {
    .comments-theme-comment-item__replies {
      margin-left: 3rem;
    }
  }

  .comments-theme-comment-item {
    position: relative;
  }

  .comments-theme-comment-item__reply-line {
    position: absolute;
    left: -0.75rem;
    top: -1rem;
    bottom: 0;
    width: 0.75rem;
    opacity: 0.5;
    z-index: 0;
    background-image: linear-gradient(
      to bottom,
      var(--pano-color-border) 2.25rem,
      transparent 2.25rem
    );
    background-size: 2px 100%;
    background-repeat: no-repeat;
  }

  @media (min-width: 576px) {
    .comments-theme-comment-item__reply-line {
      left: -1.5rem;
      width: 1.5rem;
    }
  }

  .comments-theme-comment-item__reply-line:not(.is-last) {
    background-image:
      linear-gradient(to bottom, var(--pano-color-border) 2.25rem, transparent 2.25rem),
      linear-gradient(to bottom, transparent 3.75rem, var(--pano-color-border) 3.75rem);
  }

  .comments-theme-comment-item__reply-line::after {
    content: '';
    position: absolute;
    top: 2.25rem;
    left: 0;
    width: 0.75rem;
    height: 1.5rem;
    border-bottom: 2px solid var(--pano-color-border);
    border-left: 2px solid var(--pano-color-border);
    border-bottom-left-radius: 12px;
    background: transparent;
  }

  @media (min-width: 576px) {
    .comments-theme-comment-item__reply-line::after {
      width: 1.5rem;
    }
  }
</style>

<div class="comments-theme-comment-item" class:ms-sm-3={isReply} class:ms-2={isReply}>
  {#if isReply}
    <!-- Reply Line Connector -->
    <div class="comments-theme-comment-item__reply-line" class:is-last={isLast}></div>
  {/if}

  <div>
    <div class="d-flex gap-3">
      <div class="flex-shrink-0">
        <a href="/player/{comment.username}" class="focus-ring rounded-circle">
          <img
            src="/api/v1/profile/picture/{comment.username}?{$avatarVersion}"
            class="comments-theme-comment-item__image rounded-circle"
            width="32"
            height="32"
            alt={comment.username} />
        </a>
      </div>
      <div class="flex-grow-1">
        <div class="d-flex justify-content-between align-items-center">
          <div>
            <a class="text-decoration-none fw-bold text-reset" href="/player/{comment.username}"
              >{comment.username}</a>
            {#if comment.userGroup && comment.userGroup.name !== 'default'}
              <span class="comments-theme-comment-item__badge badge ms-1">
                {comment.userGroup.displayName}
              </span>
            {/if}
          </div>
          <div class="d-flex align-items-center gap-2">
            <div class="d-none d-sm-block small opacity-50">
              <Date time={comment.createdAt} />
            </div>
            <div class="d-block d-sm-none opacity-50">
              <Date time={comment.createdAt}>
                <i class="fas fa-calendar-alt"></i>
              </Date>
            </div>
            {#if session?.user?.username === comment.username}
              <button
                class="comments-theme-comment-item__action btn btn-link link-danger ms-2 pe-0"
                type="button"
                on:click={() => deleteComment(comment.id)}
                title={$_('pages.comments.table.delete')}
                aria-label={$_('pages.comments.table.delete')}>
                <i class="fas fa-trash-alt"></i>
              </button>
            {/if}
          </div>
        </div>
        <p class="mb-0">
          {comment.content}
        </p>

        <div class="d-flex gap-3 align-items-center my-2">
          {#if !isReply && session?.user && repliesEnabled && canReply && commentsEnabled}
            <button
              title={$_('components.comments.reply')}
              aria-label={$_('components.comments.reply')}
              class="comments-theme-comment-item__reply btn btn-link"
              type="button"
              on:click={() => toggleReply(comment)}>
              <i class="fas fa-reply"></i>
            </button>
          {/if}
          {#if likesEnabled}
            <button
              class="comments-theme-comment-item__like btn btn-link text-decoration-none ps-0"
              class:link-danger={comment.isLiked}
              type="button"
              on:click={() => toggleLike(comment)}
              title={$_('components.comments.like')}
              aria-label={$_('components.comments.like')}
              disabled={!session?.user}>
              <i class="{comment.isLiked ? 'fas' : 'far'} fa-heart me-1"></i>
              {comment.likes > 0 ? comment.likes : ''}
            </button>
          {/if}
        </div>

        {#if comment.showReplyBox}
          <div class="my-2">
            <div class="d-flex gap-2">
              <textarea
                class="comments-theme-comment-item__input form-control"
                rows="2"
                autocomplete="off"
                placeholder={$_('components.comments.reply-placeholder')}
                style="resize: none;"
                bind:value={comment.replyContent}></textarea>
              <div class="hstack gap-2">
                <button
                  class="btn-close"
                  type="button"
                  on:click={() => toggleReply(comment)}
                  title={$_('components.comments.cancel')}
                  aria-label={$_('components.comments.cancel')}></button>
                <button
                  class="comments-theme-comment-item__send btn btn-primary"
                  type="button"
                  on:click={() => addComment(comment.id, comment.replyContent)}
                  title={$_('components.comments.send')}
                  aria-label={$_('components.comments.send')}
                  disabled={!comment.replyContent?.trim()}>
                  <i class="fas fa-paper-plane"></i>
                </button>
              </div>
            </div>
          </div>
        {/if}
      </div>
    </div>
  </div>

  <div class="comments-theme-comment-item__replies py-2" class:d-none={comment.replies?.length === 0}>
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
          {canReply}
          {commentsEnabled} />
      {/each}
    {/if}

    {#if !isReply && (comment.repliesHasMore !== undefined ? comment.repliesHasMore : comment.replyCount > (comment.replies?.length || 0))}
      <div class="ms-5 ps-3 mt-2">
        <button
          class="comments-theme-comment-item__load-more-replies btn btn-secondary btn-sm"
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
  import { derived } from 'svelte/store';
  import { _ as i18n } from '@panomc/sdk/utils/language';
  import { avatarVersion } from '../host.js';
  import { Date } from '@panomc/sdk/components/theme';

  // plugin translations: $_('key') reads plugins.pano-plugin-comments.key
  const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-comments.${key}`, options));


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
  export let commentsEnabled = true;

  // We use <svelte:self> for recursion
</script>
