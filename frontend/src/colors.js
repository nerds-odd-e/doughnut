export const colors = {
  sourceBackground: '#ffaaaa',
  targetBackground: '#d4ffaa',
  source: '#773333',
  target: '#447733',
};

export const bgcolors = function (role) {
  return colors[`${role}Background`];
};

export default bgcolors;
