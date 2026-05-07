import type { Note } from '@generated/doughnut-backend-api'
import type { RelationTypeLabel } from './relationTypeLabel'
import Builder from './Builder'
import NoteBuilder from './NoteBuilder'

class RelationshipBuilder extends Builder<Note> {
  internalType: RelationTypeLabel = 'related to'

  type(t: RelationTypeLabel): RelationshipBuilder {
    this.internalType = t
    return this
  }

  do(): Note {
    return new NoteBuilder().relationType(this.internalType).do()
  }
}

export default RelationshipBuilder
