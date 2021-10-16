class MindmapSector {
    offset: number
    constructor(n: number) {
        this.offset = n
    }

    get x(): number {
        return this.offset - 150 / 2
    }

    get y(): number {
        return 0 - 50 / 2
    }

    getChildSector(index: number): MindmapSector {
        return new MindmapSector(250 * (index % 2 == 0 ? 1 : -1))
    }

}

export default MindmapSector