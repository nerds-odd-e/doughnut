import type {
  Note,
  NoteRealm,
  NoteTopology,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'

function realmSlugFromNoteTopology(note: Note): string {
  const ids: number[] = []
  let t: NoteTopology | undefined = note.noteTopology
  while (t) {
    ids.push(t.id)
    t = t.parentOrSubjectNoteTopology
  }
  ids.reverse()
  return ids.map((id) => `s${id}`).join('/')
}

class NoteRealmBuilder extends Builder<NoteRealm> {
  data: NoteRealm

  noteBuilder

  constructor() {
    super()
    this.noteBuilder = new NoteBuilder()
    const noteData = this.noteBuilder.data
    this.data = {
      id: noteData.id,
      slug: realmSlugFromNoteTopology(noteData),
      note: noteData,
      inboundReferences: [],
      relationshipsDeprecating: [],
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
    value?.relationshipsDeprecating?.push(this.data.note)
    this.data.note.parentId = value.id
    this.data.note.noteTopology.parentOrSubjectNoteTopology =
      value.note.noteTopology

    return this
  }

  do(): NoteRealm {
    this.data.note = this.noteBuilder.do()
    this.data.id = this.data.note.id
    this.data.slug = realmSlugFromNoteTopology(this.data.note)
    this.data.notebookId = this.data.note.noteTopology.notebookId
    this.data.wikiTitles ??= []
    return this.data
  }
}

export default NoteRealmBuilder
