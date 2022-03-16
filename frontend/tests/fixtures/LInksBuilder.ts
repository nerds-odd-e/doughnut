import Builder from "./Builder";
import generateId from "./generateId";
import LinkBuilder from "./LinkBuilder";
import NoteSphereBuilder from "./NoteSphereBuilder";

class LinksBuilder extends Builder {
  from = new NoteSphereBuilder().title('source note').do()

  of(linkType: Generated.LinkType): LinkBuilder<LinksBuilder> {
    const child = new LinkBuilder(
      this, linkType, this.from, new NoteSphereBuilder().title(`target note ${generateId()}`).do()
    );
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