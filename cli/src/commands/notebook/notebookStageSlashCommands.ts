import type {
  CommandDoc,
  InteractiveSlashCommand,
} from '../interactiveSlashCommand.js'

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
  leaveNotebookStageSlashCommand,
]
