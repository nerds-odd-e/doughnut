import LinksMap from "./LinksMap";
import { taggingTypes, groupedTypes } from "./linkTypeOptions";

class LinksReader {
  links: LinksMap;

  constructor(links: LinksMap) {
    this.links = links;
  }

  private filterTypes(cond: (t: Generated.LinkType) => boolean) {
    return Object.fromEntries(
      Object.entries(this.links).filter((t) =>
        cond(t[0] as Generated.LinkType),
      ),
    );
  }

  get hierachyLinks() {
    const tTypes = taggingTypes;
    const gTypes = groupedTypes;
    return this.filterTypes((t) => !tTypes.includes(t) && !gTypes.includes(t));
  }

  get childrenLinks() {
    const gTypes = groupedTypes;
    return this.filterTypes((t) => !gTypes.includes(t));
  }

  get tagLinks() {
    const tTypes = taggingTypes;
    return this.filterTypes((t) => tTypes.includes(t));
  }

  get groupedLinks() {
    const tTypes = groupedTypes;
    return Object.entries(this.links)
      .filter((t) => tTypes.includes(t[0] as Generated.LinkType))
      .map((t) => t[1]);
  }

  get directLinks() {
    return Object.fromEntries(
      Object.entries(this.links).filter(
        (t) => t[1].direct && t[1].direct.length > 0,
      ),
    ) as LinksMap;
  }

  get reverseLinks() {
    return Object.fromEntries(
      Object.entries(this.links).filter(
        (t) => t[1].reverse && t[1].reverse.length > 0,
      ),
    ) as LinksMap;
  }
}

export default LinksReader;
