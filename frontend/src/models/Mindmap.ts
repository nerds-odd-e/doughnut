import MindmapSector from "./MindmapSector"
import LinksReader from "./LinksReader";
import { Coord, StraightConnection, Vector } from "./MindmapUnits";
import MindmapMetrics from "./MindmapMetrics";
import MinimumNoteSphere from "../store/MinimumNoteSphere";
import Doughnut from "../@types/Doughnut";

type NoteFinder = (id: Doughnut.ID) => MinimumNoteSphere | undefined
class Mindmap {
    rootMindmapSector: MindmapSector

    rootNoteId: Doughnut.ID

    noteFinder

    metrics: MindmapMetrics

    constructor(scale: number, rootMindmapSector: MindmapSector, rootNoteId: Doughnut.ID, noteFinder: NoteFinder, boxWidth: number, boxHeight: number) {
      this.rootMindmapSector = rootMindmapSector
      this.rootNoteId = rootNoteId
      this.noteFinder = noteFinder
      this.metrics = new MindmapMetrics(scale, boxWidth, boxHeight)
    }

    coord(sector: MindmapSector): Coord {
      return this.metrics.coord(sector.coord)
    }

    size(): string {
      if(this.metrics.scale <= 1) return 'small'
      if(this.metrics.scale <= 2) return 'medium'
      return 'large'
    }

    connectFromParent(sector: MindmapSector): StraightConnection {
      return this.metrics.straighConnection(sector.connectionFromParent)
    }

    linkToTargetNote(from: Vector, link: Generated.Link): string | undefined {
      const noteLegacy = this.noteFinder(link.targetNote.id)
      const targetSector = this.getNoteSctor(link.targetNote.id)
      if (!targetSector) return undefined
      const {reverseLinkTypes} = new LinksReader(noteLegacy!.links)
      const inSlot = this.metrics.borderVector(targetSector.inSlot(reverseLinkTypes.length, reverseLinkTypes.indexOf(link.typeId)))
      return this.metrics.linkVectors(from, inSlot)
    }

    outSlot(from: MindmapSector, connectorCount: number, connectorIndex: number): Vector {
      return this.metrics.borderVector(from.outSlot(connectorCount, connectorIndex))
    }

    inSlot(from: MindmapSector, connectorCount: number, connectorIndex: number): Vector {
      return this.metrics.borderVector(from.inSlot(connectorCount, connectorIndex))
    }

    getNoteSctor(noteId: Doughnut.ID): MindmapSector | undefined {
      const ancestors = this.ancestorsUntilRoot(noteId)
      if(!ancestors) return undefined
      let sector = this.rootMindmapSector
      for(let i = 0; i < ancestors.length - 1; i+=1) {
        sector = sector.getChildSector(ancestors[i].childrenIds.length, ancestors[i].childrenIds.indexOf(ancestors[i+1].id))
      }
      return sector
    }

    ancestorsUntilRoot(noteId: Doughnut.ID) : Array<MinimumNoteSphere> | undefined {
      const note = this.noteFinder(noteId)
      if (!note) return undefined
      if(noteId.toString() === this.rootNoteId.toString()) return [note]
      if (!note.parentId) return undefined
      return this.ancestorsUntilRoot(note!.parentId)?.concat([note])
    }

}

export default Mindmap