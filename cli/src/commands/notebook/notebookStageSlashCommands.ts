import type { Notebook } from 'doughnut-api'
import type { InteractiveSlashCommand } from '../interactiveSlashCommand.js'
import { exportSlashCommandFor } from './exportSlashCommand.js'
import { attachNotebookSlashCommandFor } from './notebookAttachSlashCommand.js'
import { pushSlashCommandFor } from './pushSlashCommand.js'
import { syncSlashCommandFor } from './syncSlashCommand.js'

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
    syncSlashCommandFor(notebook),
    exportSlashCommandFor(notebook),
    pushSlashCommandFor(notebook),
    leaveNotebookStageSlashCommand,
  ]
}
