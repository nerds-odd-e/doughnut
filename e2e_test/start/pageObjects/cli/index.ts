import { nonInteractiveOutput } from './outputAssertions'
import { installation } from './execution'
import { backend } from './backend'
import { interactiveCli } from './interactiveCli'
import { useNotebook } from './notebookInteractiveCliSession'
import { notebookClone } from './notebookClone'
import { notebookCloneCheckout } from './notebookCloneCheckout'
import { notebookLfs } from './notebookLfs'
import { notebookRecovery } from './notebookRecovery'
import { notebookLargePublication } from './notebookLargePublication'
import { ttyAssertTerminal } from './ttyAssertTerminal'

/**
 * CLI page objects. Domain ordering:
 * - Non-interactive CLI: `runInstalledCli` (managed PTY, same geometry as interactive) then `cliAssert` via `nonInteractiveOutput()`
 * - `ttyAssertTerminal()`: PTY `cy.task` I/O + same transcript fluents as `interactiveCli()`
 * - `interactiveCli()`: typing / slash / recall helpers on the PTY session
 * - `useNotebook(title)`: `/use` then fluent `attachPdfBook` / `pastCliAssistantMessages` (notebook stage)
 * - Execution (installation)
 * - Backend (bundle, install script)
 */
export const cli = {
  nonInteractiveOutput,
  ttyAssertTerminal,
  interactiveCli,
  useNotebook,
  notebookClone,
  notebookCloneCheckout,
  notebookLfs,
  notebookRecovery,
  notebookLargePublication,
  installation,
  backend,
}
