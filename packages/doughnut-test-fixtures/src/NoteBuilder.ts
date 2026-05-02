import type {
  Note,
  NoteRealm,
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

  constructor() {
    super()
    const id = generateId()
    const notebookId = generateId()
    this.data = {
      id,
      noteTopology: {
        id,
        title: 'Note1.1.1',
        notebookId,
      },
      details: '<p>Desc</p>',
      wikidataId: '',
      deletedAt: '',
      createdAt: new Date().toISOString(),
      updatedAt: '2021-08-24T08:46:44.000+00:00',
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

  notebookName(value: string): NoteBuilder {
    this.data.noteTopology.notebookName = value
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

  under(value: NoteRealm): NoteBuilder {
    value.relationshipsDeprecating ||= []
    value.relationshipsDeprecating.push(this.data)
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
