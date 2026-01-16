<script>
    import { _ } from "../../../main.js";
    import { Date } from "@panomc/sdk/components";

    export let comment;
    export let isReply = false;
    export let isLast = false;
    export let session;
    export let deleteComment;
    export let toggleReply;
    export let toggleLike;
    export let addComment;
    export let fetchReplies;

    // We use <svelte:self> for recursion
</script>

<div class="comment-wrapper" class:ms-sm-5={isReply} class:ms-4={isReply}>
    {#if isReply}
        <!-- Reply Line Connector -->
        <div class="reply-line" class:is-last={isLast}></div>
    {/if}
    <div
        class="card rounded-4 border-0 shadow-sm animate__animated animate__fadeInUp mb-3"
    >
        <div class="card-body p-4">
            <div class="d-flex gap-3">
                <div class="flex-shrink-0">
                    <a
                        href="/player/{comment.username}"
                        class="focus-ring rounded-circle"
                    >
                        <img
                            src="https://minotar.net/avatar/{comment.username ||
                                'char'}"
                            class="rounded-circle"
                            width="40"
                            height="40"
                            alt="avatar"
                        />
                    </a>
                </div>
                <div class="flex-grow-1">
                    <div
                        class="d-flex justify-content-between align-items-center mb-2"
                    >
                        <a
                            href="/player/{comment.username}"
                            class="fw-bold text-decoration-none text-primary small"
                        >
                            {comment.username}
                        </a>
                        {#if comment.userGroup && comment.userGroup.name !== "default"}
                            <span
                                class="badge text-muted fw-normal border ms-1"
                                style="font-size: 0.7rem; padding: 0.2rem 0.4rem;"
                            >
                                {comment.userGroup.displayName}
                            </span>
                        {/if}
                        <div class="d-flex align-items-center gap-2">
                            {#if session?.user?.username === comment.username}
                                <button
                                    class="btn btn-link btn-sm p-0 border-0 text-muted opacity-50 shadow-none hover-danger"
                                    type="button"
                                    on:click={() => deleteComment(comment.id)}
                                    title={$_("pages.comments.table.delete")}
                                >
                                    <i class="fas fa-trash-alt small"></i>
                                </button>
                            {/if}
                            <small class="text-muted opacity-75 small">
                                <Date time={comment.createdAt} />
                            </small>
                        </div>
                    </div>
                    <p class="mb-0" style="line-height: 1.6;">
                        {comment.content}
                    </p>
                </div>
            </div>
        </div>
        <div class="card-footer py-3 px-4 bg-transparent border-top-0">
            <div class="d-flex gap-4 ms-5 ps-3">
                {#if !isReply && session?.user}
                    <button
                        class="btn btn-link btn-sm p-0 text-decoration-none text-muted small fw-normal opacity-75"
                        type="button"
                        on:click={() => toggleReply(comment)}
                    >
                        {$_("components.comments.reply") || "Yanıtla"}
                    </button>
                {/if}
                <button
                    class="btn btn-link btn-sm p-0 text-decoration-none small fw-normal transition-all"
                    class:text-danger={comment.isLiked}
                    class:text-muted={!comment.isLiked}
                    class:opacity-75={!comment.isLiked}
                    class:opacity-50={!session?.user}
                    type="button"
                    on:click={() => toggleLike(comment)}
                    disabled={!session?.user}
                >
                    <i class="{comment.isLiked ? 'fas' : 'far'} fa-heart me-1"
                    ></i>
                    {comment.likes > 0 ? comment.likes : ""}
                </button>
            </div>

            {#if comment.showReplyBox}
                <div class="ms-5 ps-3 mt-3 animate__animated animate__fadeIn">
                    <div class="d-flex gap-2">
                        <textarea
                            class="form-control bg-body-tertiary p-2 rounded-3 shadow-none small"
                            rows="2"
                            placeholder={$_(
                                "components.comments.reply-placeholder",
                            )}
                            style="resize: none;"
                            bind:value={comment.replyContent}
                        ></textarea>
                        <button
                            class="btn btn-primary btn-sm align-self-end px-3"
                            type="button"
                            on:click={() =>
                                addComment(comment.id, comment.replyContent)}
                            disabled={!comment.replyContent?.trim()}
                            >{$_("components.comments.send")}</button
                        >
                    </div>
                </div>
            {/if}
        </div>
    </div>

    <div class="replies-container" class:d-none={comment.replies?.length === 0}>
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
                />
            {/each}
        {/if}

        {#if !isReply && (comment.repliesHasMore !== undefined ? comment.repliesHasMore : comment.replyCount > (comment.replies?.length || 0))}
            <div class="ms-5 ps-3 mt-2">
                <button
                    class="btn btn-link btn-sm p-0 text-decoration-none small fw-medium"
                    on:click={() => fetchReplies(comment)}
                    disabled={comment.repliesLoading}
                >
                    {#if comment.repliesLoading}
                        <span
                            class="spinner-border spinner-border-sm me-1"
                            role="status"
                        ></span>
                    {/if}
                    {$_("components.comments.load-more-replies", {
                        values: {
                            count:
                                comment.replyCount -
                                (comment.replies?.length || 0),
                        },
                    })}
                </button>
            </div>
        {/if}
    </div>
</div>

<style>
    .replies-container {
        position: relative;
        margin-left: 3rem;
    }

    .comment-wrapper {
        position: relative;
    }

    .reply-line {
        position: absolute;
        left: -1.5rem;
        top: -1rem;
        bottom: 0;
        width: 1.5rem;
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

    .reply-line:not(.is-last) {
        background-image: linear-gradient(
                to bottom,
                var(--bs-border-color) 2.25rem,
                transparent 2.25rem
            ),
            linear-gradient(
                to bottom,
                transparent 3.75rem,
                var(--bs-border-color) 3.75rem
            );
    }

    .reply-line::after {
        content: "";
        position: absolute;
        top: 2.25rem;
        left: 0;
        width: 1.5rem;
        height: 1.5rem;
        border-bottom: 2px solid var(--bs-border-color);
        border-left: 2px solid var(--bs-border-color);
        border-bottom-left-radius: 12px;
        background: transparent;
    }
</style>
