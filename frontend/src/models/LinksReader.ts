class LinksReader {

    linkTypeOptions: Array<any>

    links: any

    constructor(linkTypeOptions: Array<any>, links: any) {
        this.linkTypeOptions = linkTypeOptions
        this.links = links
    }

    get taggingTypes() {
        return this.linkTypeOptions
            .filter((t) => t.value == 8)
            .map((t) => t.label);
    }

    get groupedTypes() {
    return this.linkTypeOptions
        .filter((t) => [1, 12, 22, 23].includes(parseInt(t.value)))
        .map((t) => t.label);
    };

    get hierachyLinks() {
    const tTypes = this.taggingTypes
    const gTypes = this.groupedTypes
    return Object.fromEntries(
        Object.entries(this.links).filter(
        (t) => !tTypes.includes(t[0]) && !gTypes.includes(t[0])
        )
    )
    }


}

export default LinksReader