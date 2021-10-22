import Builder from "./Builder";

class LinkBuilder extends Builder {
  linkType: string;

  cnt: number;

  isReverse: boolean;

  fromNote: any

  toNote: any

  constructor(parentBuilder: Builder | undefined, linkType: string) {
    super(parentBuilder);
    this.linkType = linkType;
    this.cnt = 1;
    this.isReverse = false;
    this.fromNote = {
      id: "2423",
      title: "a tool",
    }
    this.toNote = {
      id: "2423",
      title: "a tool",
    }
  }

  count(cnt: number): LinkBuilder {
    this.cnt = cnt;
    return this;
  }

  from(note: any): LinkBuilder {
    this.fromNote = note
    return this
  }

  to(note: any): LinkBuilder {
    this.toNote = note
    return this
  }

  get reverse(): LinkBuilder {
    this.isReverse = true;
    return this;
  }

  do(): any {
    return {
      [this.linkType]: {
        [this.isReverse ? "reverse" : "direct"]: Array.from(
          { length: this.cnt },
          (x, i) => ({
            id: "1938",
            targetNote: {
              id: this.toNote.id,
              title: this.toNote.title,
            },
            sourceNote: {
              id: this.fromNote.id,
              title: this.fromNote.title,
            },
            typeId: 15,
            linkTypeLabel: "using",
            linkNameOfSource: "user",
          })
        ),
        [this.isReverse ? "direct" : "reverse"]: [],
      },
    };
  }
}

export default LinkBuilder;
