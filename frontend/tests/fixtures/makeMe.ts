class LinkBuilder {
  linkType: string;

  cnt: number;

  constructor(linkType: string) {
    this.linkType = linkType;
    this.cnt = 1;
  }

  count(cnt: number) {
    this.cnt = cnt;
    return this;
  }

  please() {
    return {
      [this.linkType]: {
        direct: Array.from({ length: this.cnt }, (x, i) => ({
          id: 1938,
          targetNote: {
            id: 2423,
            title: "a tool",
          },
          typeId: 15,
          linkTypeLabel: "using",
          linkNameOfSource: "user",
        })),
        reverse: [],
      },
    };
  }
}
const makeMe = {
  links: {
    of(linkType: string): LinkBuilder {
      return new LinkBuilder(linkType);
    },
  },
};

export default makeMe;
