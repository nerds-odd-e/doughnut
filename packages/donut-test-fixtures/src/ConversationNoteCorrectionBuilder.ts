import type {
  ConversationNoteCorrection,
  Note,
} from '@generated/donut-backend-api'
import Builder from './Builder'
import generateId from './generateId'
import NoteBuilder from './NoteBuilder'

class ConversationNoteCorrectionBuilder extends Builder<ConversationNoteCorrection> {
  data: ConversationNoteCorrection = {
    id: generateId(),
    sourceMessageId: generateId(),
    note: new NoteBuilder().please(),
    responseId: 'response-1',
    toolCallId: 'call-1',
    field: 'CONTENT',
    proposedValue: 'Clarified note content',
    reason: 'Makes the distinction explicit',
    baseRevision: 'a'.repeat(40),
    decision: 'PENDING',
  }

  forNote(note: Note) {
    this.data.note = note
    return this
  }

  fromMessage(sourceMessageId: number) {
    this.data.sourceMessageId = sourceMessageId
    return this
  }

  content(value: string) {
    this.data.field = 'CONTENT'
    this.data.proposedValue = value
    return this
  }

  title(value: string) {
    this.data.field = 'TITLE'
    this.data.proposedValue = value
    return this
  }

  reason(value: string) {
    this.data.reason = value
    return this
  }

  do() {
    return this.data
  }
}

export default ConversationNoteCorrectionBuilder
