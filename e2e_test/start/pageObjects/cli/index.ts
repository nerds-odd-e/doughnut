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
  editWorkspaceNoteBody,
  emptyWorkspace,
  removeWorkspaceFile,
  resolveWorkspaceNames,
  workspaceFileShouldHold,
  workspaceShouldNotContain,
  workspaceMatchingNotebook,
  workspaceShouldHoldOnly,
  writeWorkspaceFile,
} from './workspace'
import { pullIntoWorkspace, pullIntoWorkspaceWithinSeconds } from './syncPull'
import { ttyAssertTerminal } from './ttyAssertTerminal'

/**
 * CLI page objects. Domain ordering:
 * - Non-interactive CLI: `runInstalledCli` (managed PTY, same geometry as interactive) then `cliAssert` via `nonInteractiveOutput()`
 * - `ttyAssertTerminal()`: PTY `cy.task` I/O + same transcript fluents as `interactiveCli()`
 * - `interactiveCli()`: typing / slash / recall helpers on the PTY session
 * - `useNotebook(title)`: `/use` then fluent `attachPdfBook` / `pastCliAssistantMessages` (notebook stage)
 * - `workspace`: a local Markdown workspace as files, for `/sync`, `/push` and `/lint`
 * - `syncPull`: `/sync` pulling a notebook down into a workspace
 * - `exportDestination`: a local directory and `/export` writing the notebook into it
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  ttyAssertTerminal,
  interactiveCli,
  useNotebook,
  workspace: {
    editWorkspaceNoteBody,
    emptyWorkspace,
    removeWorkspaceFile,
    resolveWorkspaceNames,
    workspaceFileShouldHold,
    workspaceShouldNotContain,
    workspaceMatchingNotebook,
    workspaceShouldHoldOnly,
    writeWorkspaceFile,
  },
  syncPull: {
    pullIntoWorkspace,
    pullIntoWorkspaceWithinSeconds,
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
