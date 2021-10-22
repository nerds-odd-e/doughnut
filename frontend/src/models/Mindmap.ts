import MindmapSector from "./MindmapSector"

class Mindmap {
    scale: number

    rootMindmapSector: MindmapSector

    rootNoteId: number | string

    noteFinder: (id: number | string) => any

    boxWidth: number

    boxHeight: number

    constructor(scale: number, rootMindmapSector: MindmapSector, rootNoteId: number | string, noteFinder: (id: number | string)=>any, boxWidth: number, boxHeight: number) {
      this.scale = scale
      this.rootMindmapSector = rootMindmapSector
      this.rootNoteId = rootNoteId
      this.noteFinder = noteFinder
      this.boxWidth = boxWidth
      this.boxHeight = boxHeight
    }

    coord(sector: MindmapSector): any {
      return sector.coord(this.boxWidth, this.boxHeight, this.scale)
    }

    connectFromParent(sector: MindmapSector): any {
      return sector.connection(this.boxWidth, this.boxHeight, this.scale)
    }

    connection(from: MindmapSector, targetNoteId: number | string): any {
      const targetSector = this.getNoteSctor(targetNoteId)
      if (!targetSector) return undefined
      return from.linkTo(targetSector, this.scale)
    }

    outSlot(from: MindmapSector, connectorCount: number, connectorIndex: number): any {
      return from.outSlot(connectorCount, connectorIndex, this.scale, this.boxWidth, this.boxHeight)
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