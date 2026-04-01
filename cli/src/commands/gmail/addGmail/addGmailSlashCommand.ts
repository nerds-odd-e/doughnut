import { AddGmailStage } from './AddGmailStage.js'
import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../../interactiveSlashCommand.js'

const addGmailDoc: CommandDoc = {
  name: '/add gmail',
  usage: '/add gmail',
  description: 'Connect a Gmail account (OAuth)',
}

export const addGmailSlashCommand: InteractiveSlashCommand = {
  literal: '/add gmail',
  doc: addGmailDoc,
  stageComponent: AddGmailStage,
}
