import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'
import { RecallSessionStage } from './RecallSessionStage.js'

const recallDoc: CommandDoc = {
  name: '/recall',
  usage: '/recall',
  description: 'Recall the next due note (just review when no quiz is pending)',
}

export const recallSlashCommand: InteractiveSlashCommand = {
  literal: '/recall',
  doc: recallDoc,
  stageComponent: RecallSessionStage,
}
