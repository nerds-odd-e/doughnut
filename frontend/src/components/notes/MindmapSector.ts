class MindmapSector {
    nx: number

    ny: number

    startAngle: number

    angle: number

    constructor(x: number, y: number, startAngle: number, angle: number) {
        this.nx = x
        this.ny = y
        this.startAngle = startAngle
        this.angle = angle
    }

    get x(): number {
        return this.nx - 150 / 2
    }

    get y(): number {
        return this.ny - 50 / 2
    }

    getChildSector(siblingCount: number, index: number): MindmapSector {
        return new MindmapSector(this.nx + 250 * (index % 2 === 0 ? 1 : -1), 0, 0, 360)
    }

}

export default MindmapSector