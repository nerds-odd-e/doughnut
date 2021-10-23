const splitAngle =(startAngle: number, angleRange: number, siblingCount: number, index: number): number => startAngle + angleRange / siblingCount * index + angleRange / siblingCount / 2

interface Vector {
    x: number

    y: number

    angle: number
}

const borderPoint = (vector: Vector, scale: number, boxWidth: number, boxHeight: number): Vector => {
    let dy = boxHeight / 2 * Math.sign(Math.sin(vector.angle))
    let dx = dy / Math.tan(vector.angle)
    if (Number.isNaN(dx) || Math.abs(dx) > boxWidth / 2) {
        dx = boxWidth / 2 * Math.sign(Math.cos(vector.angle))
        dy = dx * Math.tan(vector.angle)
    }
    return { x: Math.round(vector.x * scale + dx), y: Math.round(vector.y * scale + dy), angle: vector.angle}
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

    connection(boxWidth: number, boxHeight: number, scale: number): any {
        const p2p = { x1: this.parentX! * scale, y1: this.parentY! * scale, x2: this.nx * scale, y2: this.ny * scale}
        const dx = p2p.x2 - p2p.x1
        const dy = p2p.y2 - p2p.y1
        let boxDx; let boxDy
        if (Math.abs(dx * boxHeight) > Math.abs(boxWidth * dy)) {
            boxDx = boxWidth / 2 * (dx > 0 ? 1 : -1)
            boxDy = dy * boxDx / dx
        }
        else {
            boxDy = boxHeight / 2 * (dy > 0 ? 1 : -1)
            boxDx = dx * boxDy / dy
        }
        return { x1: this.parentX! * scale + boxDx, y1: p2p.y1 + boxDy, x2: p2p.x2 - boxDx, y2: p2p.y2 - boxDy}
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

    outSlot(connectorCount: number, connectorIndex: number, scale: number, boxWidth: number, boxHeight: number): Vector {
        const start = this.startAngle + this.angleRange / 2 - Math.PI
        return this.connectPoint(start, connectorCount, connectorIndex, scale, boxWidth, boxHeight)
    }

    inSlot(connectorCount: number, connectorIndex: number, scale: number, boxWidth: number, boxHeight: number): Vector {
        const start = this.startAngle + this.angleRange / 2
        return this.connectPoint(start, connectorCount, connectorIndex, scale, boxWidth, boxHeight)
    }

    private connectPoint(start: number, connectorCount: number, connectorIndex: number, scale: number, boxWidth: number, boxHeight: number): Vector {
        const range = Math.PI - this.angleRange / 2
        const angle = splitAngle(start, range, connectorCount, connectorIndex)
        return borderPoint({angle, x: this.nx, y: this.ny}, scale, boxWidth, boxHeight)
    }

}

export default MindmapSector
export { Vector, MindmapSector }