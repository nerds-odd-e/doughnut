import type { NoteRealm } from "@/generated/backend"
import Builder from "./Builder"
import NoteBuilder from "./NoteBuilder"
import generateId from "./generateId"

class NoteRealmBuilder extends Builder<NoteRealm> {
  data: NoteRealm

  noteBuilder

  constructor() {
    super()
    this.noteBuilder = new NoteBuilder()
    const noteData = this.noteBuilder.data
    this.data = {
      id: noteData.id,
      note: noteData,
      refers: [],
      children: [],
    }
  }

  topicConstructor(value: string): NoteRealmBuilder {
    this.noteBuilder.topicConstructor(value)
    return this
  }

  inCircle(circleName: string) {
    this.data.circle = {
      id: generateId(),
      name: circleName,
    }
    return this
  }

  wikidataId(value: string): NoteRealmBuilder {
    this.noteBuilder.wikidataId(value)
    return this
  }

  details(value: string): NoteRealmBuilder {
    this.noteBuilder.details(value)
    return this
  }

  image(value: string): NoteRealmBuilder {
    this.noteBuilder.image(value)
    return this
  }

  under(value: NoteRealm): NoteRealmBuilder {
    value?.children?.push(this.data.note)
    this.data.note.parentId = value.id
    this.data.note.noteTopic.parentNoteTopic = value.note.noteTopic

    return this
  }

  updatedAt(value: Date): NoteRealmBuilder {
    this.noteBuilder.updatedAt(value)
    return this
  }

  do(): NoteRealm {
    this.data.note = this.noteBuilder.do()
    return this.data
  }
}

export default NoteRealmBuilder
