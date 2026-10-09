// Plugin-level options of the Pano plugin kit (@panomc/plugin-kit). The namespace is `comments` (the plugin id minus
// `pano-plugin-`); the views are the files of src/theme/views.
export default {
  styles: {
    // The comment and reply boxes keep their fixed-size textareas (`resize: none`) as inline style.
    styleAttrAllow: ['AddCommentModal', 'ThemeCommentItem'],
  },
};
