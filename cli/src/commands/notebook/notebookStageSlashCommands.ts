import type { Notebook } from 'doughnut-api'
import type { InteractiveSlashCommand } from '../interactiveSlashCommand.js'
import { attachNotebookSlashCommandFor } from './notebookAttachSlashCommand.js'

const leaveNotebookDoc = {
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

export function notebookStageSlashCommandsFor(
  notebook: Notebook
): readonly InteractiveSlashCommand[] {
  return [
    attachNotebookSlashCommandFor(notebook),
    leaveNotebookStageSlashCommand,
  ]
}
