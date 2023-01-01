import LinksMap from "../../src/models/LinksMap";
import Builder from "./Builder";
import generateId from "./generateId";

class LinkViewedBuilder extends Builder<LinksMap> {
  linkType: Generated.LinkType;

  cnt: number;

  isReverse: boolean;

  fromNote: Generated.NoteRealm;

  toNote: Generated.NoteRealm;

  constructor(
    linkType: Generated.LinkType,
    from: Generated.NoteRealm,
    to: Generated.NoteRealm
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
    if (!this.fromNote.links || !this.toNote.links)
      throw new Error("note does not have links");
    if (!this.toNote.links.links[this.linkType])
      this.toNote.links.links[this.linkType] = { direct: [], reverse: [] };
    const linksOfType = this.toNote.links.links[this.linkType];
    if (linksOfType && !linksOfType.reverse) linksOfType.reverse = [];
    linksOfType?.reverse.push(this.link());

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
      linkType: "using",
    };
  }
}

export default LinkViewedBuilder;
export type { LinksMap };
