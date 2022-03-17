import LinksMap from "../../src/models/LinksMap";
import Builder from "./Builder";
import generateId from "./generateId";

class LinkBuilder extends Builder<LinksMap> {
  linkType: Generated.LinkType;

  cnt: number;

  isReverse: boolean;

  fromNote: Generated.NoteSphere

  toNote: Generated.NoteSphere

  constructor(
    linkType: Generated.LinkType,
    from: Generated.NoteSphere,
    to: Generated.NoteSphere
  ) {
    super();
    this.linkType = linkType;
    this.cnt = 1;
    this.isReverse = false;
    this.fromNote = from;
    this.toNote = to;
  }

  count(cnt: number) {
    this.cnt = cnt;
    return this;
  }

  get reverse() {
    this.isReverse = true;
    return this;
  }

  do(): LinksMap {
    if (!this.fromNote.links || !this.toNote.links) throw new Error('note does not have links');
    if (!this.toNote.links[this.linkType]) this.toNote.links[this.linkType] = { direct: [], reverse: [] }
    const linksOfType = this.toNote.links[this.linkType]
    if (linksOfType && !linksOfType.reverse) linksOfType.reverse = []
    linksOfType?.reverse.push(this.link())

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

  private link(): Generated.Link {
    return {
      id: generateId(),
      targetNote: this.toNote.note,
      sourceNote: this.fromNote.note,
      typeId: 15,
      linkTypeLabel: "using",
      linkNameOfSource: "user",
      createdAt: '',
    }
  }

}

export default LinkBuilder;
export { LinksMap }
