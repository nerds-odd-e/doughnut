import type { Note, NoteTopology as NoteTopologyType } from "@generated/backend"
// Using string literals for relationType values
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"

class LinkBuilder extends Builder<Note> {
  sourceNoteBuilder = new NoteBuilder()

  targetNoteBuilder = new NoteBuilder()

  internalType: NoteTopologyType["relationType"] = "related to"

  from(note: Note): LinkBuilder {
    this.sourceNoteBuilder.data = note
    return this
  }

  to(note: Note): LinkBuilder {
    this.targetNoteBuilder.data = note
    return this
  }

  type(t: NoteTopologyType["relationType"]): LinkBuilder {
    this.internalType = t
    return this
  }

  do(): Note {
    return new NoteBuilder()
      .relationType(this.internalType)
      .underNote(this.sourceNoteBuilder.do())
      .target(this.targetNoteBuilder.do())
      .do()
  }
}

export default LinkBuilder
