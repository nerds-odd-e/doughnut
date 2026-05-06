import type {
  Note,
  RelationshipCreation,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import generateId from './generateId'

/** Same rule as backend RelationshipNoteMarkdownFormatter.labelToKebab */
function relationLabelToKebab(label: string): string {
  const t = label.trim()
  if (!t) return 'related-to'
  return t.toLowerCase().replace(/\s+/g, '-')
}

class NoteBuilder extends Builder<Note> {
  data: Note

  /** Notebook id for paired NoteRealm fixtures; not part of API `NoteTopology`. */
  realmNotebookId: number

  constructor() {
    super()
    const id = generateId()
    this.realmNotebookId = generateId()
    this.data = {
      id,
      noteTopology: {
        id,
        title: 'Note1.1.1',
        createdAt: new Date().toISOString(),
        updatedAt: '2021-08-24T08:46:44.000+00:00',
      },
      details: '<p>Desc</p>',
      wikidataId: '',
      deletedAt: '',
    }
  }

  for(note: Note | undefined) {
    if (note) {
      this.data = note
    }
    return this
  }

  title(value: string): NoteBuilder {
    this.data.noteTopology.title = value
    return this
  }

  wikidataId(value: string): NoteBuilder {
    this.data.wikidataId = value
    return this
  }

  details(value: string | undefined): NoteBuilder {
    this.data.details = value
    return this
  }

  folder(folderId: number): NoteBuilder {
    this.data.noteTopology.folderId = folderId
    return this
  }

  createdAt(value: string): NoteBuilder {
    this.data.noteTopology.createdAt = value
    return this
  }

  updatedAt(value: string): NoteBuilder {
    this.data.noteTopology.updatedAt = value
    return this
  }

  relationType(value: RelationshipCreation['relationType']): NoteBuilder {
    this.title(`:${value}`)
    const kebab = relationLabelToKebab(value)
    this.data.details = `---\nrelation: ${kebab}\n---\n`
    return this
  }

  do(): Note {
    return this.data
  }
}

export default NoteBuilder
