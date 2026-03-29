import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { RecallJustReviewStage } from './RecallJustReviewStage.js'

const recallDoc: CommandDoc = {
  name: '/recall',
  usage: '/recall',
  description: 'Recall the next due note (just review when no quiz is pending)',
}

export const recallSlashCommand: InteractiveSlashCommand = {
  line: '/recall',
  doc: recallDoc,
  stageComponent: RecallJustReviewStage,
}
