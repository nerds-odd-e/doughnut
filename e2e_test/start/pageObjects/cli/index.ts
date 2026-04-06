import { nonInteractiveOutput } from './nonInteractiveOutputAssertions'
import { installation } from './execution'
import { backend } from './backend'
import { interactiveCli } from './interactiveCli'
import { useNotebook } from './notebookInteractiveCliSession'
import { ttyAssertTerminal } from './ttyAssertTerminal'

/**
 * CLI page objects. Domain ordering:
 * - Non-interactive stdout (`nonInteractiveOutputAssertions`); PTY transcript assertions (`outputAssertions`)
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
  installation,
  backend,
}
