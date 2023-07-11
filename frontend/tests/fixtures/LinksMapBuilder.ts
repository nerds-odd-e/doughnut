import Builder from "./Builder";
import generateId from "./generateId";
import LinkViewedBuilder from "./LinkViewedBuilder";
import NoteRealmBuilder from "./NoteRealmBuilder";

class LinksMapBuilder extends Builder<Generated.LinksOfANote> {
  from = new NoteRealmBuilder().title("source note").do();

  protected childrenBuilders: Omit<LinkViewedBuilder, "please">[] = [];

  of(linkType: Generated.LinkType) {
    const child = new LinkViewedBuilder(
      linkType,
      this.from,
      new NoteRealmBuilder().title(`target note ${generateId()}`).do(),
    ).parent(this);
    this.childrenBuilders.push(child);
    return child;
  }

  do(): Generated.LinksOfANote {
    return {
      links: this.childrenBuilders.reduce(
        (prev, curr) => ({ ...prev, ...curr.do() }),
        {},
      ),
    };
  }
}

export default LinksMapBuilder;
