import Builder from "./Builder";

class LinkBuilder<PB extends Builder> extends Builder<any, PB> {
  linkType: string;

  cnt: number;

  isReverse: boolean;

  fromNote: any

  toNote: any

  constructor(parentBuilder: PB | undefined, linkType: string) {
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
      links: {},
    }
  }

  count(cnt: number) {
    this.cnt = cnt;
    return this;
  }

  from(note: any) {
    this.fromNote = note
    return this
  }

  to(note: any) {
    this.toNote = note
    return this
  }

  get reverse() {
    this.isReverse = true;
    return this;
  }

  do(): any {
    if (!this.toNote.links[this.linkType]) this.toNote.links[this.linkType] = {}
    if (!this.toNote.links[this.linkType].reverse) this.toNote.links[this.linkType].reverse = []
    this.toNote.links[this.linkType].reverse.push(this.link())

    return {
      [this.linkType]: {
        [this.isReverse ? "reverse" : "direct"]: Array.from(
          { length: this.cnt },
          () => this.link()
        ),
        [this.isReverse ? "direct" : "reverse"]: [],
      },
    };
  }

  private link(): any {
    return {
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
          }
  }

}

export default LinkBuilder;
