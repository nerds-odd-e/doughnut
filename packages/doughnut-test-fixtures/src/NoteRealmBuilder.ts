import type {
  NoteRealm,
  NotebookClientView,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'

class NoteRealmBuilder extends Builder<NoteRealm> {
  data: NoteRealm

  noteBuilder

  constructor() {
    super()
    this.noteBuilder = new NoteBuilder()
    const noteData = this.noteBuilder.data
    const ts = new Date().toISOString()
    const notebook = {
      id: this.noteBuilder.realmNotebookId,
      name: noteData.noteTopology.title,
      notebookSettings: { skipMemoryTrackingEntirely: false },
      createdAt: ts,
      updatedAt: ts,
    }
    const notebookView: NotebookClientView = {
      notebook,
      readonly: false,
    }
    this.data = {
      id: noteData.id,
      note: noteData,
      references: [],
      wikiTitles: [],
      notebookView,
      ancestorFolders: [],
    }
  }

  title(value: string): NoteRealmBuilder {
    this.noteBuilder.title(value)
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

  folder(folderId: number): NoteRealmBuilder {
    this.noteBuilder.folder(folderId)
    return this
  }

  image(value: string): NoteRealmBuilder {
    this.noteBuilder.image(value)
    return this
  }

  under(value: NoteRealm): NoteRealmBuilder {
    value?.references?.push(this.data.note.noteTopology)
    return this
  }

  do(): NoteRealm {
    this.data.note = this.noteBuilder.do()
    this.data.id = this.data.note.id
    const nb = this.data.notebookView.notebook
    if (nb) {
      nb.id = this.noteBuilder.realmNotebookId
      nb.name = this.data.note.noteTopology.title
    }
    this.data.wikiTitles ??= []
    return this.data
  }
}

export default NoteRealmBuilder
