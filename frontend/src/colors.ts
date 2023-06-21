export interface IColors {
  [property: string]: string;
}

export const colors: IColors = {
  sourceBackground: "#ffaaaa",
  targetBackground: "#d4ffaa",
  source: "#773333",
  target: "#447733",
};

export const bgcolors = (role: string): string =>
  colors[`${role}Background`] as string;
