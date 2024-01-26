import Builder from "./Builder";
import generateId from "./generateId";
import LinkViewedBuilder, { LinksMap } from "./LinkViewedBuilder";
import NoteRealmBuilder from "./NoteRealmBuilder";

class LinksMapBuilder extends Builder<LinksMap> {
  from = new NoteRealmBuilder().topicConstructor("source note").do();

  protected childrenBuilders: Omit<LinkViewedBuilder, "please">[] = [];

  of(linkType: Generated.LinkType) {
    const child = new LinkViewedBuilder(
      linkType,
      this.from,
      new NoteRealmBuilder()
        .topicConstructor(`target note ${generateId()}`)
        .do(),
    ).parent(this);
    this.childrenBuilders.push(child);
    return child;
  }

  do(): LinksMap {
    return this.childrenBuilders.reduce(
      (prev, curr) => ({ ...prev, ...curr.do() }),
      {},
    );
  }
}

export default LinksMapBuilder;
