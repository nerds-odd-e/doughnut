import { MindmapSector, Vector, StraightConnection } from "./MindmapSector"
import LinksReader from "./LinksReader";

class MindmapMetrics {
    scale: number

    boxWidth: number

    boxHeight: number

    constructor(scale: number, boxWidth: number, boxHeight: number) {
      this.scale = scale
      this.boxWidth = boxWidth
      this.boxHeight = boxHeight
    }

    borderVector(vector: Vector): Vector {
        let dy = this.boxHeight / 2 * Math.sign(Math.sin(vector.angle))
        let dx = dy / Math.tan(vector.angle)
        if (Number.isNaN(dx) || Math.abs(dx) > this.boxWidth / 2) {
            dx = this.boxWidth / 2 * Math.sign(Math.cos(vector.angle))
            dy = dx * Math.tan(vector.angle)
        }
        return { x: Math.round(vector.x * this.scale + dx), y: Math.round(vector.y * this.scale + dy), angle: vector.angle}
    }

    straighConnection(conn: StraightConnection): StraightConnection {
        const p2p = { x1: conn.x1 * this.scale, y1: conn.y1 * this.scale, x2: conn.x2 * this.scale, y2: conn.y2 * this.scale}
        const dx = p2p.x2 - p2p.x1
        const dy = p2p.y2 - p2p.y1
        let boxDx; let boxDy
        if (Math.abs(dx * this.boxHeight) > Math.abs(this.boxWidth * dy)) {
            boxDx = this.boxWidth / 2 * (dx > 0 ? 1 : -1)
            boxDy = dy * boxDx / dx
        }
        else {
            boxDy = this.boxHeight / 2 * (dy > 0 ? 1 : -1)
            boxDx = dx * boxDy / dy
        }
        return { x1: p2p.x1 + boxDx, y1: p2p.y1 + boxDy, x2: p2p.x2 - boxDx, y2: p2p.y2 - boxDy}
    }

}

class Mindmap {
    scale: number

    rootMindmapSector: MindmapSector

    rootNoteId: number | string

    noteFinder: (id: number | string) => any

    boxWidth: number

    boxHeight: number

    metrics: MindmapMetrics

    constructor(scale: number, rootMindmapSector: MindmapSector, rootNoteId: number | string, noteFinder: (id: number | string)=>any, boxWidth: number, boxHeight: number) {
      this.scale = scale
      this.rootMindmapSector = rootMindmapSector
      this.rootNoteId = rootNoteId
      this.noteFinder = noteFinder
      this.boxWidth = boxWidth
      this.boxHeight = boxHeight
      this.metrics = new MindmapMetrics(scale, boxWidth, boxHeight)
    }

    coord(sector: MindmapSector): any {
      return sector.coord(this.boxWidth, this.boxHeight, this.scale)
    }

    connectFromParent(sector: MindmapSector): StraightConnection {
      return this.metrics.straighConnection(sector.connectionFromParent)
    }

    linkToTargetNote(from: any, link: any): any {
      const note = this.noteFinder(link.targetNote.id)
      const targetSector = this.getNoteSctor(link.targetNote.id)
      if (!targetSector) return undefined
      const {reverseLinkTypes} = new LinksReader(note.links)
      const inSlot = this.metrics.borderVector(targetSector.inSlot(reverseLinkTypes.length, reverseLinkTypes.indexOf(link.linkTypeId)))
      return this.linkVectors(from, inSlot)
    }

    outSlot(from: MindmapSector, connectorCount: number, connectorIndex: number): Vector {
      return this.metrics.borderVector(from.outSlot(connectorCount, connectorIndex))
    }

    getNoteSctor(noteId: number | string): MindmapSector | undefined {
      const ancestors = this.ancestorsUntilRoot(noteId)
      if(!ancestors) return undefined
      let sector = this.rootMindmapSector
      for(let i = 0; i < ancestors.length - 1; i+=1) {
        sector = sector.getChildSector(ancestors[i].childrenIds.length, ancestors[i].childrenIds.indexOf(ancestors[i+1].id))
      }
      return sector
    }

    ancestorsUntilRoot(noteId: number | string) : Array<any> | undefined {
      const note = this.noteFinder(noteId)
      if(noteId.toString() === this.rootNoteId.toString()) return [note]
      if (!note.parentId) return undefined
      return this.ancestorsUntilRoot(note.parentId)?.concat([note])
    }

    private linkVectors(from: Vector, to: Vector): any {
        return {
           x2: to.x * this.scale,
           y2: to.y * this.scale,
           x1: from.x * this.scale,
           y1: from.y * this.scale
        }
    }


}

export default Mindmap