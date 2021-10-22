class MindmapSector {
    readonly radius = 210

    parentX: number | null = null

    parentY: number | null = null

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

    linkTo(target: MindmapSector, scale: number): any {
        return { x1: this.nx * scale, y1: this.ny * scale, x2: target.nx * scale, y2: target.ny * scale}
    }

    coord(boxWidth: number, boxHeight: number, scale: number): any {
        return { x: this.nx * scale - boxWidth / 2, y: this.ny * scale - boxHeight / 2}
    }

    getChildSector(siblingCount: number, index: number): MindmapSector {
        const ang = this.startAngle + this.angle / siblingCount * index + this.angle / siblingCount / 2
        const x = this.nx + this.radius * Math.cos(ang)
        const y = this.ny + this.radius * Math.sin(ang)
        const start = this.startAngle + this.angle / siblingCount * index - Math.PI / 10
        const child = new MindmapSector(x, y, start, this.angle / siblingCount + Math.PI / 5)
        child.parentX = this.nx
        child.parentY = this.ny
        return child
    }

}

export default MindmapSector
