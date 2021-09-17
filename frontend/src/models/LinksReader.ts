class LinksReader {

    linkTypeOptions: Array<any>

    links: any

    constructor(linkTypeOptions: Array<any>, links: any) {
        this.linkTypeOptions = linkTypeOptions
        this.links = links
    }

    get groupedTypes() {
    return this.linkTypeOptions
        .filter((t) => [1, 8, 12, 22, 23].includes(parseInt(t.value, 10)))
        .map((t) => t.label);
    };

    get hierachyLinks() {
    const gTypes = this.groupedTypes
    return Object.fromEntries(
        Object.entries(this.links).filter(
        (t) => !gTypes.includes(t[0])
        )
    )
    }

    get childrenLinks(){
        const gTypes = this.groupedTypes
        return Object.fromEntries(
            Object.entries(this.links).filter(
            (t) => !gTypes.includes(t[0])
            )
        )
    }

    get groupedLinks(){
    const tTypes = this.groupedTypes
    return Object.entries(this.links)
        .filter((t) => tTypes.includes(t[0]))
        .map((t) => t[1]);
    }

    reverseLabel(lbl: string): string {
        const { reversedLabel } = this.linkTypeOptions.find(
            ({ label }) => lbl === label
        );
        return reversedLabel;
    };


}

export default LinksReader