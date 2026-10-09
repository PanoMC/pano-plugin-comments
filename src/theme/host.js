// Host handles the theme views need from the plugin entry. A view cannot import main.js (it holds the plugin class and the
// panel pages), so main.js hands the avatar version store over here when the plugin loads.
// Until then (and in a theme that ejected a view without running the plugin) the version is empty and avatars still load.
let version = null;

/** @param {{ subscribe: Function } | null | undefined} store the host's avatar version store */
export function setAvatarVersion(store) {
  version = store ?? null;
}

/** Svelte store contract: `$avatarVersion` in a view reads the host store, else ''. */
export const avatarVersion = {
  subscribe(run) {
    if (version) return version.subscribe(run);

    run('');

    return () => {};
  },
};
