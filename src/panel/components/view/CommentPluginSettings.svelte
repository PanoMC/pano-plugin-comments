<script>
    import {_} from "../../../main.js";
    import ApiUtil from "@panomc/sdk/utils/api";
    import {showToast} from "@panomc/sdk/toasts";

    export let addon;

    let config = addon?.config || {
        repliesEnabled: true,
        likesEnabled: true,
        paginationType: "PAGINATION",
        commentPermission: "",
        replyPermission: "",
        cooldown: 0,
        inputStyle: "MODAL",
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
                path: "/api/panel/comment/config",
                body: config,
            });
            addon.config = config; // Update local state for consistency
            initialConfig = JSON.parse(JSON.stringify(config)); // Update initial config to new saved state
            await showToast("components.toasts.settings-save-success");
        } catch (e) {
            console.error("Failed to save comment config", e);
            await showToast("components.toasts.settings-save-error");
        } finally {
            saving = false;
        }
    }
</script>

{#if addon?.id === "pano-plugin-comments"}
    <h6 class="mb-3">
        {$_("settings.title", {default: "Comment Settings"})}
    </h6>

    <div class="vstack gap-3">
        <!-- Replies -->
        <div class="d-flex align-items-center justify-content-between">
            <div>
                <label class="form-label mb-0" for="repliesEnabled">
                    {$_("settings.replies-enabled", {
                        default: "Enable Replies",
                    })}
                </label>
                <div class="small">
                    {$_("settings.replies-enabled-desc", {
                        default: "Allow users to reply to comments",
                    })}
                </div>
            </div>
            <div class="form-check form-switch">
                <input
                        class="form-check-input"
                        type="checkbox"
                        id="repliesEnabled"
                        bind:checked={config.repliesEnabled}
                />
            </div>
        </div>

        <hr class="my-0 opacity-10"/>

        <!-- Likes -->
        <div class="d-flex align-items-center justify-content-between">
            <div>
                <label class="form-label mb-0" for="likesEnabled">
                    {$_("settings.likes-enabled", {
                        default: "Enable Likes",
                    })}
                </label>
                <div class="small">
                    {$_("settings.likes-enabled-desc", {
                        default: "Allow users to like comments",
                    })}
                </div>
            </div>
            <div class="form-check form-switch">
                <input
                        class="form-check-input"
                        type="checkbox"
                        id="likesEnabled"
                        bind:checked={config.likesEnabled}
                />
            </div>
        </div>

        <hr class="my-0 opacity-10"/>

        <!-- Comment Permission -->
        <div>
            <label class="form-label" for="commentPermission">
                {$_("settings.comment-permission", {
                    default: "Comment Permission",
                })}
            </label>
            <input
                    type="text"
                    class="form-control"
                    id="commentPermission"
                    placeholder="pano.plugin.comments.create"
                    bind:value={config.commentPermission}
            />
            <div class="form-text">
                {$_("settings.comment-permission-desc", {
                    default: "Leave empty to allow everyone to comment",
                })}
            </div>
        </div>

        <!-- Reply Permission -->
        <div>
            <label class="form-label" for="replyPermission">
                {$_("settings.reply-permission", {
                    default: "Reply Permission",
                })}
            </label>
            <input
                    type="text"
                    class="form-control"
                    id="replyPermission"
                    placeholder="pano.plugin.comments.reply"
                    bind:value={config.replyPermission}
            />
            <div class="form-text">
                {$_("settings.reply-permission-desc", {
                    default: "Leave empty to allow everyone to reply",
                })}
            </div>
        </div>

        <hr class="my-0 opacity-10"/>

        <!-- Cooldown -->
        <div>
            <label class="form-label" for="cooldown">
                {$_("settings.cooldown", {
                    default: "Cooldown (Seconds)",
                })}
            </label>
            <input
                    type="number"
                    class="form-control"
                    id="cooldown"
                    min="0"
                    bind:value={config.cooldown}
            />
            <div class="form-text">
                {$_("settings.cooldown-desc", {
                    default: "Minimum time (in seconds) between comments",
                })}
            </div>
        </div>

        <hr class="my-0 opacity-10"/>

        <!-- Input Style -->
        <div>
            <span class="form-label d-block mb-2">
                {$_("settings.input-style", {
                    default: "Input Style",
                })}
            </span>
            <div class="btn-group w-100" role="group">
                <input
                        type="radio"
                        class="btn-check"
                        name="inputStyle"
                        id="inputStyle1"
                        value="MODAL"
                        bind:group={config.inputStyle}
                />
                <label class="btn btn-outline-primary" for="inputStyle1">
                    <i class="fas fa-window-maximize me-2"></i>
                    {$_("settings.input-style-modal", {
                        default: "Modal",
                    })}
                </label>

                <input
                        type="radio"
                        class="btn-check"
                        name="inputStyle"
                        id="inputStyle2"
                        value="INLINE"
                        bind:group={config.inputStyle}
                />
                <label class="btn btn-outline-primary" for="inputStyle2">
                    <i class="fas fa-pen me-2"></i>
                    {$_("settings.input-style-inline", {
                        default: "Inline Input",
                    })}
                </label>
            </div>
        </div>

        <hr class="my-0 opacity-10"/>

        <!-- Pagination -->
        <div>
            <span class="form-label d-block mb-2">
                {$_("settings.pagination-type", {
                    default: "Pagination Type",
                })}
            </span>
            <div class="btn-group w-100" role="group">
                <input
                        type="radio"
                        class="btn-check"
                        name="paginationType"
                        id="paginationType1"
                        value="PAGINATION"
                        bind:group={config.paginationType}
                />
                <label class="btn btn-outline-primary" for="paginationType1">
                    <i class="fas fa-list-ol me-2"></i>
                    {$_("settings.pagination-classic", {
                        default: "Classic Pagination",
                    })}
                </label>

                <input
                        type="radio"
                        class="btn-check"
                        name="paginationType"
                        id="paginationType2"
                        value="LOAD_MORE"
                        bind:group={config.paginationType}
                />
                <label class="btn btn-outline-primary" for="paginationType2">
                    <i class="fas fa-arrow-down me-2"></i>
                    {$_("settings.pagination-load-more", {
                        default: "Load More",
                    })}
                </label>
            </div>
        </div>

        <div class="d-flex justify-content-end mt-2">
            <button
                    class="btn btn-primary"
                    on:click={saveConfig}
                    disabled={saving || !hasChanges}
            >
                {#if saving}
                    <span
                            class="spinner-border spinner-border-sm me-2"
                            role="status"
                            aria-hidden="true"
                    ></span>
                {:else}
                    <i class="fas fa-save me-2"></i>
                {/if}
                {$_("generic.save", {default: "Save"})}
            </button>
        </div>
    </div>
{/if}
