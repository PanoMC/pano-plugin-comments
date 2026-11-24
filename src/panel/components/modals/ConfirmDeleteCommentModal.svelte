<!-- Confirm Delete Comment Modal -->
<div aria-hidden="true" class="modal fade" bind:this={$modalElement} role="dialog" tabindex="-1">
  <div class="modal-dialog modal-dialog-centered" role="dialog">
    <div class="modal-content">
      <div class="modal-body text-center">
        <div class="pb-3">
          <i class="fas fa-question-circle fa-3x d-block m-auto text-gray"></i>
        </div>
        {$_('components.comments.modals.delete.description')}
      </div>
      <div class="modal-footer flex-nowrap">
        <button
          class="btn btn-link col-6 m-0 text-decoration-none"
          type="button"
          class:disabled={loading}
          on:click={hide}>
          {$_('components.comments.modals.delete.cancel')}
        </button>
        <button
          class="btn btn-danger col-6 m-0"
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
    import { api } from '@panomc/sdk/plugin-api';

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
    modal.hide();
  }
</script>

<script>
  import { _, showSuccessToast, showErrorToast } from '../../../main';

  let loading = false;

  async function onYesClick() {
    loading = true;

    try {
      const res = await api.panel.delete({
        path: `/comments/${get(commentId)}`,
      });
      if (res.error) {
        console.error('Failed to delete comment', res.error);
        showErrorToast($_('toasts.delete-error'));
      } else {
        callback(get(commentId));
        hide();
        showSuccessToast($_('toasts.delete-success'));
      }
    } catch (e) {
      console.error('Failed to delete comment', e);
      showErrorToast($_('toasts.delete-error'));
    } finally {
      loading = false;
    }
  }
</script>
