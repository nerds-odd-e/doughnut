import type { Note, NoteTopology as NoteTopologyType } from "@generated/backend"
import { NoteTopology } from "@/managedApi/noteTopologyConstants"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"

class LinkBuilder extends Builder<Note> {
  sourceNoteBuilder = new NoteBuilder()

  targetNoteBuilder = new NoteBuilder()

  internalType: NoteTopologyType["linkType"] = NoteTopology.linkType.RELATED_TO

  from(note: Note): LinkBuilder {
    this.sourceNoteBuilder.data = note
    return this
  }

  to(note: Note): LinkBuilder {
    this.targetNoteBuilder.data = note
    return this
  }

  type(t: NoteTopologyType["linkType"]): LinkBuilder {
    this.internalType = t
    return this
  }

  do(): Note {
    return new NoteBuilder()
      .linkType(this.internalType)
      .underNote(this.sourceNoteBuilder.do())
      .target(this.targetNoteBuilder.do())
      .do()
  }
}

export default LinkBuilder
