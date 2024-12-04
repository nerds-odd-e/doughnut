import type { Note } from "@/generated/backend"
import { NoteTopology } from "@/generated/backend"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"

class LinkBuilder extends Builder<Note> {
  sourceNoteBuilder = new NoteBuilder()

  targetNoteBuilder = new NoteBuilder()

  internalType = NoteTopology.linkType.RELATED_TO

  from(note: Note): LinkBuilder {
    this.sourceNoteBuilder.data = note
    return this
  }

  to(note: Note): LinkBuilder {
    this.targetNoteBuilder.data = note
    return this
  }

  type(t: NoteTopology.linkType): LinkBuilder {
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
