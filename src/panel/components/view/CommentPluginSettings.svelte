{#if addon?.id === 'pano-plugin-comments'}
  <div class="card">
    <div class="card-header">
      {$_('settings.title', { default: 'Preferences' })}
    </div>
    <div class="card-body">
      <!-- Replies -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="repliesEnabled">
          <span class="d-block">
            {$_('settings.replies-enabled', {
              default: 'Enable Replies',
            })}
          </span>
          <small>
            {$_('settings.replies-enabled-desc', {
              default: 'Allow users to reply to comments',
            })}
          </small>
        </label>
        <div class="col-md-6 d-flex align-items-center">
          <div class="form-check form-switch">
            <input
              class="form-check-input"
              type="checkbox"
              id="repliesEnabled"
              bind:checked={config.repliesEnabled} />
          </div>
        </div>
      </div>

      <!-- Likes -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="likesEnabled">
          <span class="d-block">
            {$_('settings.likes-enabled', {
              default: 'Enable Likes',
            })}
          </span>
          <small>
            {$_('settings.likes-enabled-desc', {
              default: 'Allow users to like comments',
            })}
          </small>
        </label>
        <div class="col-md-6 d-flex align-items-center">
          <div class="form-check form-switch">
            <input
              class="form-check-input"
              type="checkbox"
              id="likesEnabled"
              bind:checked={config.likesEnabled} />
          </div>
        </div>
      </div>

      <!-- Comment Permission -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="commentPermission">
          {$_('settings.comment-permission', {
            default: 'Comment Permission',
          })}
        </label>
        <div class="col-md-6">
          <input
            type="text"
            class="form-control"
            id="commentPermission"
            placeholder="pano.plugin.comments.create"
            bind:value={config.commentPermission} />
          <small class="d-block mt-2">
            {$_('settings.comment-permission-desc', {
              default: 'Leave empty to allow everyone to comment',
            })}
          </small>
        </div>
      </div>

      <!-- Reply Permission -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="replyPermission">
          {$_('settings.reply-permission', {
            default: 'Reply Permission',
          })}
        </label>
        <div class="col-md-6">
          <input
            type="text"
            class="form-control"
            id="replyPermission"
            placeholder="pano.plugin.comments.reply"
            bind:value={config.replyPermission} />
          <small class="d-block mt-2">
            {$_('settings.reply-permission-desc', {
              default: 'Leave empty to allow everyone to reply',
            })}
          </small>
        </div>
      </div>

      <!-- Cooldown -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="cooldown">
          {$_('settings.cooldown', {
            default: 'Cooldown (Seconds)',
          })}
        </label>
        <div class="col-md-6">
          <input
            type="number"
            class="form-control"
            id="cooldown"
            min="0"
            bind:value={config.cooldown} />
          <small class="d-block mt-2">
            {$_('settings.cooldown-desc', {
              default: 'Minimum time (in seconds) between comments',
            })}
          </small>
        </div>
      </div>

      <!-- Input Style -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="inputStyle1">
          {$_('settings.input-style', {
            default: 'Input Style',
          })}
        </label>
        <div class="col-md-6">
          <div class="btn-group w-100" role="group">
            <input
              type="radio"
              class="btn-check"
              name="inputStyle"
              id="inputStyle1"
              value="MODAL"
              bind:group={config.inputStyle} />
            <label class="btn btn-outline-primary" for="inputStyle1">
              {$_('settings.input-style-modal', {
                default: 'Modal',
              })}
            </label>

            <input
              type="radio"
              class="btn-check"
              name="inputStyle"
              id="inputStyle2"
              value="INLINE"
              bind:group={config.inputStyle} />
            <label class="btn btn-outline-primary" for="inputStyle2">
              {$_('settings.input-style-inline', {
                default: 'Inline Input',
              })}
            </label>
          </div>
        </div>
      </div>

      <!-- Pagination -->
      <div class="row mb-3">
        <label class="col-md-6 col-form-label" for="paginationType1">
          {$_('settings.pagination-type', {
            default: 'Pagination Type',
          })}
        </label>
        <div class="col-md-6">
          <div class="btn-group w-100" role="group">
            <input
              type="radio"
              class="btn-check"
              name="paginationType"
              id="paginationType1"
              value="PAGINATION"
              bind:group={config.paginationType} />
            <label class="btn btn-outline-primary" for="paginationType1">
              {$_('settings.pagination-classic', {
                default: 'Classic Pagination',
              })}
            </label>

            <input
              type="radio"
              class="btn-check"
              name="paginationType"
              id="paginationType2"
              value="LOAD_MORE"
              bind:group={config.paginationType} />
            <label class="btn btn-outline-primary" for="paginationType2">
              {$_('settings.pagination-load-more', {
                default: 'Load More',
              })}
            </label>
          </div>
        </div>
      </div>

      <div class="mt-4">
        <button class="btn btn-secondary" on:click={saveConfig} disabled={saving || !hasChanges}>
          {#if saving}
            <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"
            ></span>
          {:else}{/if}
          {$_('generic.save', { default: 'Save' })}
        </button>
      </div>
    </div>
  </div>
{/if}

<script>
  import { _ } from '../../../main.js';
  import ApiUtil from '@panomc/sdk/utils/api';
  import { showToast } from '@panomc/sdk/toasts';

  export let addon;

  let config = addon?.config || {
    repliesEnabled: true,
    likesEnabled: true,
    paginationType: 'PAGINATION',
    commentPermission: '',
    replyPermission: '',
    cooldown: 0,
    inputStyle: 'MODAL',
  };
  let saving = false;

  // Keep track of initial config to determine if there are changes
  let initialConfig = JSON.parse(JSON.stringify(config));

  $: hasChanges = JSON.stringify(config) !== JSON.stringify(initialConfig);

  async function saveConfig() {
    if (saving) return;
    saving = true;
    try {
      await ApiUtil.put({
        path: '/api/panel/comment/config',
        body: config,
      });
      addon.config = config; // Update local state for consistency
      initialConfig = JSON.parse(JSON.stringify(config)); // Update initial config to new saved state
      await showToast('components.toasts.settings-save-success');
    } catch (e) {
      console.error('Failed to save comment config', e);
      await showToast('components.toasts.settings-save-error');
    } finally {
      saving = false;
    }
  }
</script>
