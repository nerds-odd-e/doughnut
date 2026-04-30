import type { NoteRealm } from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'

class NoteRealmBuilder extends Builder<NoteRealm> {
  data: NoteRealm

  noteBuilder

  constructor() {
    super()
    this.noteBuilder = new NoteBuilder()
    const noteData = this.noteBuilder.data
    this.data = {
      id: noteData.id,
      slug: noteData.noteTopology.slug,
      note: noteData,
      inboundReferences: [],
      children: [],
      wikiTitles: [],
      notebookId: noteData.noteTopology.notebookId,
    }
  }

  title(value: string): NoteRealmBuilder {
    this.noteBuilder.title(value)
    return this
  }

  notebookName(value: string): NoteRealmBuilder {
    this.noteBuilder.notebookName(value)
    return this
  }

  createdAt(value: string): NoteRealmBuilder {
    this.noteBuilder.createdAt(value)
    return this
  }

  updatedAt(value: string): NoteRealmBuilder {
    this.noteBuilder.updatedAt(value)
    return this
  }

  updatedAtDate(value: Date): NoteRealmBuilder {
    this.noteBuilder.updatedAt(value.toJSON())
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
    this.data.note.noteTopology.parentOrSubjectNoteTopology =
      value.note.noteTopology

    return this
  }

  do(): NoteRealm {
    this.data.note = this.noteBuilder.do()
    this.data.id = this.data.note.id
    this.data.slug = this.data.note.noteTopology.slug
    this.data.notebookId = this.data.note.noteTopology.notebookId
    this.data.wikiTitles ??= []
    return this.data
  }
}

export default NoteRealmBuilder
