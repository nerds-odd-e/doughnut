const splitAngle =(startAngle: number, angleRange: number, siblingCount: number, index: number): number => startAngle + angleRange / siblingCount * index + angleRange / siblingCount / 2

interface Vector {
    x: number

    y: number

    angle: number
}

interface StraightConnection {
    x1: number

    y1: number

    x2: number

    y2: number
}

class MindmapSector {
    readonly radius = 210

    parentX: number | null = null

    parentY: number | null = null

    nx: number

    ny: number

    startAngle: number

    angleRange: number

    constructor(x: number, y: number, startAngle: number, angleRange: number) {
        this.nx = x
        this.ny = y
        this.startAngle = startAngle
        this.angleRange = angleRange
    }

    get isHead(): boolean {
        return this.parentX === null
    }

    get connectionFromParent(): StraightConnection {
        return { x1: this.parentX!, y1: this.parentY!, x2: this.nx, y2: this.ny}
    }

    coord(boxWidth: number, boxHeight: number, scale: number): any {
        return { x: this.nx * scale - boxWidth / 2, y: this.ny * scale - boxHeight / 2}
    }


    getChildSector(siblingCount: number, index: number, extraScale = 1): MindmapSector {
        const ang = splitAngle(this.startAngle, this.angleRange, siblingCount, index)
        const x = this.nx + this.radius * Math.cos(ang) * extraScale
        const y = this.ny + this.radius * Math.sin(ang) * extraScale
        const start = this.startAngle + this.angleRange / siblingCount * index - Math.PI / 10
        const child = new MindmapSector(x, y, start, this.angleRange / siblingCount + Math.PI / 5)
        child.parentX = this.nx
        child.parentY = this.ny
        return child
    }

    outSlot(connectorCount: number, connectorIndex: number): Vector {
        const start = this.startAngle + this.angleRange / 2 - Math.PI
        return this.connectPoint(start, connectorCount, connectorIndex)
    }

    inSlot(connectorCount: number, connectorIndex: number): Vector {
        const start = this.startAngle + this.angleRange / 2
        return this.connectPoint(start, connectorCount, connectorIndex)
    }

    private connectPoint(start: number, connectorCount: number, connectorIndex: number): Vector {
        const range = Math.PI - this.angleRange / 2
        const angle = splitAngle(start, range, connectorCount, connectorIndex)
        return {angle, x: this.nx, y: this.ny}
    }

}

export default MindmapSector
export { Vector, StraightConnection, MindmapSector }