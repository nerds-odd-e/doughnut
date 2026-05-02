import type {
  Note,
  RelationshipCreation,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'

class RelationshipBuilder extends Builder<Note> {
  sourceNoteBuilder = new NoteBuilder()

  internalType: RelationshipCreation['relationType'] = 'related to'

  from(note: Note): RelationshipBuilder {
    this.sourceNoteBuilder.data = note
    return this
  }

  type(t: RelationshipCreation['relationType']): RelationshipBuilder {
    this.internalType = t
    return this
  }

  do(): Note {
    return new NoteBuilder()
      .relationType(this.internalType)
      .underNote(this.sourceNoteBuilder.do())
      .do()
  }
}

export default RelationshipBuilder
