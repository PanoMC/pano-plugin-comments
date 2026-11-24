<!-- Confirm Delete Comment Modal -->
<div aria-hidden="true" class="comments-confirm-delete-comment-modal modal fade" bind:this={$modalElement} role="dialog" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered" role="dialog">
    <div class="modal-content">
      <div class="comments-confirm-delete-comment-modal__body modal-body text-center">
        <div class="pb-3">
          <i class="fas fa-question-circle fa-3x d-block m-auto text-gray"></i>
        </div>
        {$_('components.comments.modals.delete.description')}
      </div>
      <div class="comments-confirm-delete-comment-modal__footer modal-footer flex-nowrap">
        <button
          class="comments-confirm-delete-comment-modal__action btn btn-link col-6 m-0 text-decoration-none"
          type="button"
          class:disabled={loading}
          on:click={hide}>
          {$_('components.comments.modals.delete.cancel')}
        </button>
        <button
          class="comments-confirm-delete-comment-modal__confirm btn btn-danger col-6 m-0"
          type="button"
          class:disabled={loading}
          on:click={onYesClick}>
          {$_('components.comments.modals.delete.confirm')}
        </button>
      </div>
    </div>
  </div>
</div>

<script context="module">
    import {get, writable} from 'svelte/store';

    const modalElement = writable();
  const commentId = writable(null);

  let callback = (id) => {};
  let modal;

  export function show(id) {
    commentId.set(id);

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
  import { derived } from 'svelte/store';
  import { _ as i18n } from '@panomc/sdk/utils/language';
  import { showToast } from '@panomc/sdk/toasts';
  import { api } from '@panomc/sdk/plugin-api';

  // plugin translations: $_('key') reads plugins.pano-plugin-comments.key
  const _ = derived(i18n, ($_fn) => (key, options) => $_fn(`plugins.pano-plugin-comments.${key}`, options));

  // toasts coloured like the panel's: success / danger variants (an older host ignores the option and renders neutral)
  const showSuccessToast = (text, params = {}) => showToast(text, params, undefined, { variant: 'success' });
  const showErrorToast = (text, params = {}) => showToast(text, params, undefined, { variant: 'danger' });

  let loading = false;

  async function onYesClick() {
    loading = true;
    const id = get(commentId);
    try {
      const res = await api.delete({
        path: `/comments/${id}`,
      });
      if (!res.error) {
        callback(id);
        hide();
        showSuccessToast($_('toasts.delete-success'));
      } else {
        showErrorToast($_('toasts.delete-error'));
      }
    } catch (e) {
      console.error('Failed to delete comment', e);
      showErrorToast($_('toasts.delete-error'));
    } finally {
      loading = false;
    }
  }
</script>
