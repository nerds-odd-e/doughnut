import LinksMap from "./LinksMap";
import { taggingTypes, groupedTypes } from "./linkTypeOptions";

class LinksReader {
  links: LinksMap;

  constructor(links: LinksMap) {
    this.links = links;
  }

  get hierachyLinks() {
    const tTypes = taggingTypes;
    const gTypes = groupedTypes;
    return Object.fromEntries(
      Object.entries(this.links).filter(
        (t) => !tTypes.includes(t[0]) && !gTypes.includes(t[0])
      )
    );
  }

  get childrenLinks() {
    const gTypes = groupedTypes;
    return Object.fromEntries(
      Object.entries(this.links).filter((t) => !gTypes.includes(t[0]))
    );
  }

  get tagLinks() {
    const tTypes = taggingTypes;
    return Object.fromEntries(
      Object.entries(this.links).filter((t) => tTypes.includes(t[0]))
    );
  }

  get groupedLinks() {
    const tTypes = groupedTypes;
    return Object.entries(this.links)
      .filter((t) => tTypes.includes(t[0]))
      .map((t) => t[1]);
  }

  get directLinks() {
    return Object.fromEntries(
      Object.entries(this.links).filter(
        (t) => t[1].direct && t[1].direct.length > 0
      )
    );
  }

  get reverseLinks() {
    return Object.fromEntries(
      Object.entries(this.links).filter(
        (t) => t[1].reverse && t[1].reverse.length > 0
      )
    );
  }
}

export default LinksReader;
