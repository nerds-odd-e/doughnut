export const colors = {
  sourceBackground: '#ffaaaa',
  targetBackground: '#d4ffaa',
  source: '#773333',
  target: '#447733',
};

const bgcolors = (role) => colors[`${role}Background`];

export default bgcolors;
