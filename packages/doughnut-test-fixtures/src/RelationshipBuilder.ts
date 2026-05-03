import type {
  Note,
  RelationshipCreation,
} from '@generated/doughnut-backend-api'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'

class RelationshipBuilder extends Builder<Note> {
  internalType: RelationshipCreation['relationType'] = 'related to'

  type(t: RelationshipCreation['relationType']): RelationshipBuilder {
    this.internalType = t
    return this
  }

  do(): Note {
    return new NoteBuilder().relationType(this.internalType).do()
  }
}

export default RelationshipBuilder
