import Builder from "./Builder";
import LinkBuilder from "./LinkBuilder";

class LinksBuilder extends Builder {
  of(linkType: string): LinkBuilder {
    const child = new LinkBuilder(this, linkType);
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
