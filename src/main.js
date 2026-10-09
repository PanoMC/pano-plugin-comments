import { PanoPlugin, viewComponent } from '@panomc/sdk';
import { derived } from 'svelte/store';
import { _ as i18n } from '@panomc/sdk/utils/language';
import { api } from '@panomc/sdk/plugin-api';
import { showToast } from '@panomc/sdk/toasts';
import { setAvatarVersion } from './theme/host.js';

const pluginId = 'pano-plugin-comments';

// this is to render plugin translations
export const _ = derived(i18n, ($_fn) => {
  return (key, options) => $_fn(`plugins.${pluginId}.${key}`, options);
});

// Success/failure colouring for this plugin's toasts, matching the panel. showToast from
// @panomc/sdk/toasts is the host panel's ToastContainer `show`, whose signature is
// (text, params, toastComponent, options): passing undefined for toastComponent keeps the
// host's DefaultToast, and options.variant maps to Bootstrap's text-success / text-danger.
// These live here rather than in @panomc/sdk/toasts because this plugin is pinned to
// @panomc/sdk 1.0.0-dev.39, which predates the variants; they can be dropped for a direct
// SDK import once that pin moves. On an older panel build the extra argument is ignored and
// the toast renders neutral, so this degrades instead of breaking.
export function showSuccessToast(text, params = {}) {
  return showToast(text, params, undefined, { variant: 'success' });
}

export function showErrorToast(text, params = {}) {
  return showToast(text, params, undefined, { variant: 'danger' });
}

export let pano;

export default class PanoPluginComments extends PanoPlugin {
  onLoad() {
    pano = this.pano;
    // the theme views read the avatar cache-buster through theme/host.js (they cannot import this file)
    setAvatarVersion(pano.ui?.avatar?.getVersion?.());
    const panoApi = this.pano;

    if (pano.isPanel) {
      pano.ui.page.register({
        path: '/posts/comments',
        component: viewComponent(() => import('./panel/CommentsPage.svelte')),
        systemLayout: 'PostsLayout',
        resetLayout: false,
        permission: `pano.plugin.${pluginId}.manage.comments`,
      });

      pano.ui.posts.editMenu(async (items) => {
        items.push({
          href: '/posts/comments',
          text: `plugins.${pluginId}.pages.comments.title`,
          permission: `pano.plugin.${pluginId}.manage.comments`,
        });
        return items;
      });

      pano.ui.posts.onLoad(async (data, event) => {
        const postIds = data.posts.map((p) => p.id).filter((id) => id > 0);
        if (postIds.length === 0) return;

        try {
          const res = await api.panel.get({
            path: `/comment/post-counts?postIds=${postIds.join(',')}`,
            request: event,
          });
          data.commentCounts = res.counts || {};
        } catch (e) {
          console.error('Failed to load comment counts', e);
          data.commentCounts = {};
        }
      });

      pano.ui.hook.register({
        name: 'panel:post-editor:content:bottom',
        component: viewComponent(() => import('./panel/components/view/PostComments.svelte')),
        permission: `pano.plugin.${pluginId}.manage.comments`,
      });

      pano.ui.hook.register({
        name: 'panel:posts:table:header:after-views',
        component: viewComponent(
          () => import('./panel/components/view/PostTableHeaderCommentCount.svelte'),
        ),
        permission: `pano.plugin.${pluginId}.manage.comments`,
      });

      pano.ui.hook.register({
        name: 'panel:posts:table:row:after-views',
        component: viewComponent(
          () => import('./panel/components/view/PostTableRowCommentCount.svelte'),
        ),
        permission: `pano.plugin.${pluginId}.manage.comments`,
      });

      pano.ui.hook.register({
        name: 'panel:player-detail:bottom',
        component: viewComponent(() => import('./panel/components/view/PlayerComments.svelte')),
        permission: `pano.plugin.${pluginId}.manage.comments`,
      });

      pano.ui.addon.onLoad(async (data, event) => {
        if (data.addon.id !== 'pano-plugin-comments') return;

        try {
          const config = await api.panel.get({
            path: '/comment/config',
            request: event,
          });
          data.addon.config = config;
        } catch (e) {
          console.error('[pano-plugin-comments] Failed to load config', e);
        }
      });

      pano.ui.hook.register({
        name: `panel:plugin-detail:content:${pluginId}`,
        component: viewComponent(
          () => import('./panel/components/view/CommentPluginSettings.svelte'),
        ),
        permission: `pano.plugin.${pluginId}.manage.comments`,
      });
    } else {
      // here you can write your theme-related codes
      pano.ui.post.onLoad(async (data, event) => {
        if (!data.post || data.post.id <= 0) return;

        const url = new URL(event.url);
        const page = url.searchParams.get('commentsPage') || 1;

        try {
          const [commentsRes, configRes] = await Promise.all([
            api.get({
              path: `/comments/${data.post.id}?page=${page}`,
              request: event,
            }),
            api.get({
              path: `/comment/config`,
              request: event,
            }),
          ]);
          data.post.commentData = {
            comments: commentsRes.items || [],
            totalComments: commentsRes.page?.totalItems || 0,
            commentsEnabled: commentsRes.commentsEnabled,
            page,
            config: configRes,
          };
        } catch (e) {
          console.error('Failed to load comments or config', e);
          data.post.commentData = {};
        }
      });

      // the comments block under a post is the view comments:ThemePostComments (hook theme:post-detail:bottom, block: true)
    }
  }

  onContextUpdate(ctx) { }

  onUnload() { }
}
