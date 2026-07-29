import { nonInteractiveOutput } from './outputAssertions'
import { installation } from './execution'
import { backend } from './backend'
import { interactiveCli } from './interactiveCli'
import { useNotebook } from './notebookInteractiveCliSession'
import {
  addExtraDestinationFile,
  destinationFileShouldHold,
  destinationShouldHoldOnly,
  emptyDestination,
  exportNotebook,
} from './exportDestination'
import {
  addExtraWorkspaceFile,
  editWorkspaceFile,
  editWorkspaceNoteBody,
  pullIntoWorkspace,
  pullIntoWorkspaceWithinSeconds,
  removeWorkspaceFile,
  resolveWorkspaceNames,
  workspaceFileShouldHold,
  workspaceShouldNotContain,
  workspaceMatchingNotebook,
  workspaceShouldHoldOnly,
} from './syncWorkspace'
import { ttyAssertTerminal } from './ttyAssertTerminal'

/**
 * CLI page objects. Domain ordering:
 * - Non-interactive CLI: `runInstalledCli` (managed PTY, same geometry as interactive) then `cliAssert` via `nonInteractiveOutput()`
 * - `ttyAssertTerminal()`: PTY `cy.task` I/O + same transcript fluents as `interactiveCli()`
 * - `interactiveCli()`: typing / slash / recall helpers on the PTY session
 * - `useNotebook(title)`: `/use` then fluent `attachPdfBook` / `pastCliAssistantMessages` (notebook stage)
 * - `syncWorkspace`: a local Markdown workspace and `/sync --dry-run` against it
 * - `exportDestination`: a local directory and `/export` writing the notebook into it
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  ttyAssertTerminal,
  interactiveCli,
  useNotebook,
  syncWorkspace: {
    addExtraWorkspaceFile,
    editWorkspaceFile,
    editWorkspaceNoteBody,
    pullIntoWorkspace,
    pullIntoWorkspaceWithinSeconds,
    removeWorkspaceFile,
    resolveWorkspaceNames,
    workspaceFileShouldHold,
    workspaceShouldNotContain,
    workspaceMatchingNotebook,
    workspaceShouldHoldOnly,
  },
  exportDestination: {
    addExtraDestinationFile,
    destinationFileShouldHold,
    destinationShouldHoldOnly,
    emptyDestination,
    exportNotebook,
  },
  installation,
  backend,
}
