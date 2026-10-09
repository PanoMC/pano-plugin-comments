// Sample data for the view catalogue (doc 02 section 7). Pure data.
export const notApplicable = ['empty', 'loading', 'error'];

const comments = [
  {
    id: 1,
    username: 'Alex',
    content: 'Great post, thanks for sharing the update!',
    createdAt: 1767225600000,
    userGroup: { name: 'admin', displayName: 'Admin' },
    likes: 3,
    isLiked: true,
    replyCount: 2,
    repliesHasMore: false,
    replies: [
      { id: 3, username: 'Steve', content: 'Agreed.', createdAt: 1767229200000, userGroup: { name: 'default' }, likes: 0, isLiked: false, replyCount: 0, replies: [] },
      { id: 4, username: 'Notch', content: 'Same here.', createdAt: 1767232800000, userGroup: { name: 'default' }, likes: 1, isLiked: false, replyCount: 0, replies: [] },
    ],
  },
  { id: 2, username: 'Steve', content: 'When is the next event?', createdAt: 1767236400000, userGroup: { name: 'default' }, likes: 0, isLiked: false, replyCount: 0, replies: [] },
];
const config = { likesEnabled: true, repliesEnabled: true, paginationType: 'PAGINATION', commentPermission: '', replyPermission: '', inputStyle: 'MODAL' };
const noop = () => {};
const handlers = { deleteComment: noop, toggleReply: noop, toggleLike: noop, addComment: noop, fetchReplies: noop };

/** @type {import('@panomc/plugin-kit').Samples} */
export default {
  filled: { props: { comment: comments[0], session: { user: { username: 'Alex' } }, ...handlers } },
  reply: { label: 'A reply', props: { comment: comments[0].replies[0], isReply: true, isLast: true, session: null, ...handlers } },
};
