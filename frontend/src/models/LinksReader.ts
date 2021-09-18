class LinksReader {
  linkTypeOptions: Array<any>;

  links: any;

  constructor(linkTypeOptions: Array<any>, links: any) {
    this.linkTypeOptions = linkTypeOptions;
    this.links = links;
  }

  get taggingTypes() {
    return this.linkTypeOptions
      .filter((t) => parseInt(t.value, 10) === 8)
      .map((t) => t.label);
  }

  get groupedTypes() {
    return this.linkTypeOptions
      .filter((t) => [1, 12, 22, 23].includes(parseInt(t.value, 10)))
      .map((t) => t.label);
  }

  get hierachyLinks() {
    const tTypes = this.taggingTypes;
    const gTypes = this.groupedTypes;
    return Object.fromEntries(
      Object.entries(this.links).filter(
        (t) => !tTypes.includes(t[0]) && !gTypes.includes(t[0])
      )
    );
  }

  get childrenLinks() {
    const gTypes = this.groupedTypes;
    return Object.fromEntries(
      Object.entries(this.links).filter((t) => !gTypes.includes(t[0]))
    );
  }

  get tagLinks() {
    const tTypes = this.taggingTypes;
    return Object.fromEntries(
      Object.entries(this.links).filter((t) => tTypes.includes(t[0]))
    );
  }

  get groupedLinks() {
    const tTypes = this.groupedTypes;
    return Object.entries(this.links)
      .filter((t) => tTypes.includes(t[0]))
      .map((t) => t[1]);
  }

  reverseLabel(lbl: string): string {
    const linkType = this.linkTypeOptions.find(({ label }) => lbl === label);
    if (linkType) return linkType.reversedLabel;
    return "*unknown link type*";
  }
}

export default LinksReader;
