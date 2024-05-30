import { NoteRealm, Note } from "@/generated/backend";
import LinksMap from "../../src/models/LinksMap";
import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";

class LinkViewedBuilder extends Builder<LinksMap> {
  linkType: Note.linkType;

  cnt: number;

  isReverse: boolean;

  fromNote: NoteRealm;

  toNote: NoteRealm;

  constructor(linkType: Note.linkType, from: NoteRealm, to: NoteRealm) {
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
    if (!this.toNote.links[this.linkType])
      this.toNote.links[this.linkType] = { reverse: [] };
    const linksOfType = this.toNote.links[this.linkType];
    if (linksOfType && !linksOfType.reverse) linksOfType.reverse = [];
    linksOfType?.reverse.push(this.link());

    return {
      [this.linkType]: {
        [this.isReverse ? "reverse" : "direct"]: Array.from(
          { length: this.cnt },
          () => this.link(),
        ),
        [this.isReverse ? "direct" : "reverse"]: [],
      },
    };
  }

  private link(): Note {
    return new LinkBuilder()
      .from(this.fromNote.note)
      .to(this.toNote.note)
      .type(Note.linkType.USING)
      .do();
  }
}

export default LinkViewedBuilder;
export type { LinksMap };
