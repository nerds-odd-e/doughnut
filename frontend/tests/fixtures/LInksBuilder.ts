import Builder from "./Builder";
import generateId from "./generateId";
import LinkBuilder, { LinksMap } from "./LinkBuilder";
import NoteSphereBuilder from "./NoteSphereBuilder";

class LinksBuilder extends Builder<LinksMap> {
  from = new NoteSphereBuilder().title('source note').do()

  protected childrenBuilders: Omit<LinkBuilder, "please">[] = [];

  of(linkType: Generated.LinkType) {
    const child = new LinkBuilder(
      linkType, this.from, new NoteSphereBuilder().title(`target note ${generateId()}`).do()
    ).parent(this);
    this.childrenBuilders.push(child);
    return child;
  }

  do(): any {
    return this.childrenBuilders.reduce(
      (prev, curr) => ({ ...prev, ...curr.do() }),
      {}
    );
  }
}

export default LinksBuilder;