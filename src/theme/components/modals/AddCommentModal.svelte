<div aria-hidden="true" class="modal fade" bind:this={$modalElement} role="dialog" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered" role="dialog">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title">{$_('pages.comments.title')}</h5>
        <button type="button" class="btn-close" on:click={hide} aria-label={$_('components.comments.close')}></button>
      </div>
      <div class="modal-body">
        <div class="d-flex gap-2">
          <div class="flex-shrink-0 d-none d-sm-block">
            <img
              src="https://minotar.net/avatar/{session?.user?.username || 'char'}"
              class="rounded-circle"
              width="40"
              height="40"
              alt={$_('components.comments.avatar_alt')} />
          </div>
          <div class="flex-grow-1">
            <textarea
              class="form-control"
              rows="4"
              autocomplete="off"
              placeholder={$_('components.comments.placeholder')}
              style="resize: none;"
              bind:value={content}
              disabled={loading}></textarea>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button
          class="btn btn-secondary w-100"
          type="button"
          disabled={!content.trim() || loading}
          on:click={onSendClick}>
          {$_('components.comments.send')}
        </button>
      </div>
    </div>
  </div>
</div>

<script context="module">
  import {get, writable} from 'svelte/store';

  const modalElement = writable();
  let callback = (content) => {};
  let modal;

  export function show() {
    modal = new window.bootstrap.Modal(get(modalElement), {
      backdrop: 'static',
      keyboard: false,
    });
    modal.show();
  }

  export function setCallback(newCallback) {
    callback = newCallback;
  }

  export function hide() {
    if (modal) modal.hide();
  }
</script>

<script>
  import { _ } from '../../../main.js';
  import { page } from '@panomc/sdk/svelte';

  $: session = $page.data.session;
  let content = '';
  let loading = false;

  async function onSendClick() {
    if (!content.trim() || loading) return;

    loading = true;
    try {
      await callback(content);
      content = '';
      hide();
    } catch (e) {
      console.error('Failed to add comment via modal', e);
    } finally {
      loading = false;
    }
  }
</script>
