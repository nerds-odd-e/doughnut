import MindmapSector from "./MindmapSector"

class Mindmap {
    scale: number

    rootMindmapSector: MindmapSector

    rootNoteId: number | string

    noteFinder: (id: number | string) => any

    constructor(scale: number, rootMindmapSector: MindmapSector, rootNoteId: number | string, noteFinder: (id: number | string)=>any) {
      this.scale = scale
      this.rootMindmapSector = rootMindmapSector
      this.rootNoteId = rootNoteId
      this.noteFinder = noteFinder
    }

    connectFromParent(sector: MindmapSector, boxWidth: number, BoxHeight: number): any {
      return sector.connection(boxWidth, BoxHeight, this.scale)
    }

    connection(from: MindmapSector, targetNoteId: number | string): any {
      const targetSector = this.getNoteSctor(targetNoteId)
      if (!targetSector) return undefined
      return from.linkTo(targetSector, this.scale)
    }

    getNoteSctor(noteId: number | string): MindmapSector | undefined {
      const ancestors = this.ancestorsUntilRoot(noteId)
      if(!ancestors) return undefined
      let sector = this.rootMindmapSector
      for(let i = 0; i < ancestors.length - 1; i+=1) {
        sector = sector.getChildSector(ancestors[i].childrenIds.length, ancestors[1].childrenIds.indexOf(ancestors[i+1].id))
      }
      return sector
    }

    ancestorsUntilRoot(noteId: number | string) : Array<any> | undefined {
      const note = this.noteFinder(noteId)
      if(noteId === this.rootNoteId) return [note]
      if (!note.parentId) return undefined
      return this.ancestorsUntilRoot(note.parentId)?.concat([note])
    }


}

export default Mindmap