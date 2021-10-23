import MindmapSector from "./MindmapSector"
import LinksReader from "./LinksReader";
import { Coord, StraightConnection, Vector } from "./MindmapUnits";
import MindmapMetrics from "./MindmapMetrics";

class Mindmap {
    rootMindmapSector: MindmapSector

    rootNoteId: number | string

    noteFinder: (id: number | string) => any

    metrics: MindmapMetrics

    constructor(scale: number, rootMindmapSector: MindmapSector, rootNoteId: number | string, noteFinder: (id: number | string)=>any, boxWidth: number, boxHeight: number) {
      this.rootMindmapSector = rootMindmapSector
      this.rootNoteId = rootNoteId
      this.noteFinder = noteFinder
      this.metrics = new MindmapMetrics(scale, boxWidth, boxHeight)
    }

    coord(sector: MindmapSector): Coord {
      return this.metrics.coord(sector.coord)
    }

    connectFromParent(sector: MindmapSector): StraightConnection {
      return this.metrics.straighConnection(sector.connectionFromParent)
    }

    linkToTargetNote(from: Vector, link: any): string | undefined {
      const note = this.noteFinder(link.targetNote.id)
      const targetSector = this.getNoteSctor(link.targetNote.id)
      if (!targetSector) return undefined
      const {reverseLinkTypes} = new LinksReader(note.links)
      const inSlot = this.metrics.borderVector(targetSector.inSlot(reverseLinkTypes.length, reverseLinkTypes.indexOf(link.typeId)))
      return this.metrics.linkVectors(from, inSlot)
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

}

export default Mindmap