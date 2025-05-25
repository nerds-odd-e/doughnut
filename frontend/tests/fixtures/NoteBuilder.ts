import type { Note, NoteRealm } from "generated/backend"
import { NoteTopology } from "generated/backend"
import Builder from "./Builder"
import generateId from "./generateId"

class NoteBuilder extends Builder<Note> {
  data: Note

  constructor() {
    super()
    const id = generateId()
    this.data = {
      id,
      noteTopology: {
        id,
        titleOrPredicate: "Note1.1.1",
      },
      details: "<p>Desc</p>",
      wikidataId: "",
      deletedAt: "",
      createdAt: new Date().toISOString(),
      updatedAt: "2021-08-24T08:46:44.000+00:00",
    }
  }

  for(note: Note | undefined) {
    if (note) {
      this.data = note
    }
    return this
  }

  topicConstructor(value: string): NoteBuilder {
    this.data.noteTopology.titleOrPredicate = value
    return this
  }

  wikidataId(value: string): NoteBuilder {
    this.data.wikidataId = value
    return this
  }

  details(value: string | undefined): NoteBuilder {
    this.data.details = value
    this.data.noteTopology.shortDetails = `${value}, just shorter`
    return this
  }

  under(value: NoteRealm): NoteBuilder {
    value.children ||= []
    value.children.push(this.data)
    this.underNote(value.note)
    return this
  }

  underNote(value: Note): NoteBuilder {
    this.data.parentId = value.id
    this.data.noteTopology.parentOrSubjectNoteTopology = value.noteTopology
    return this
  }

  createdAt(value: string): NoteBuilder {
    this.data.createdAt = value
    return this
  }

  updatedAt(value: string): NoteBuilder {
    this.data.updatedAt = value
    return this
  }

  linkType(value: NoteTopology.linkType): NoteBuilder {
    this.topicConstructor(`:${value}`)
    // default target
    this.data.noteTopology.objectNoteTopology = {
      id: generateId(),
      linkType: value,
      titleOrPredicate: "a target",
    }
    return this
  }

  target(note: Note): NoteBuilder {
    this.data.noteTopology.objectNoteTopology = note.noteTopology
    return this
  }

  do(): Note {
    return this.data
  }
}

export default NoteBuilder
