import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'

const ATTACH_NOT_IMPLEMENTED =
  'Attach is not implemented yet. Parsing and server sync will follow in a later update.'

const attachNotebookDoc: CommandDoc = {
  name: '/attach',
  usage: '/attach <path to pdf>',
  description:
    'Attach a PDF to the active notebook (outline extraction and POST attach-book).',
}

const attachNotebookStageSlashCommand: InteractiveSlashCommand = {
  literal: '/attach',
  doc: attachNotebookDoc,
  argument: { name: 'path to PDF', optional: false },
  run() {
    return { assistantMessage: ATTACH_NOT_IMPLEMENTED }
  },
}

const leaveNotebookDoc: CommandDoc = {
  name: '/exit',
  usage: '/exit, exit',
  description: 'Leave notebook context',
}

export const leaveNotebookStageSlashCommand: InteractiveSlashCommand = {
  literal: '/exit',
  doc: leaveNotebookDoc,
  run() {
    return { assistantMessage: 'Left notebook context.' }
  },
}

export const notebookStageSlashCommands: readonly InteractiveSlashCommand[] = [
  attachNotebookStageSlashCommand,
  leaveNotebookStageSlashCommand,
]
