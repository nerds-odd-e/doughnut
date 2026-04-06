import {
  formatRawTerminalSnapshotForError,
  headPreview,
} from 'tty-assert/errorSnapshotFormatting'

/** Cypress `cy.get('@…')` handle; must match `.as(…)` on the stored stdout chain. */
export const DOUGHNUT_OUTPUT_CY_ALIAS = 'doughnutOutput'
export const OUTPUT_ALIAS = `@${DOUGHNUT_OUTPUT_CY_ALIAS}`

const SECTION = {
  nonInteractive: 'non-interactive output',
} as const

const WRONG_NON_INTERACTIVE_STEP =
  'Expected non-interactive CLI output (e.g. `version` / `update` spawn), but this capture looks like an interactive PTY session.'

function failCliAssertion(message: string, raw: string): never {
  throw new Error(
    `${message}\n\n--- CLI terminal snapshot (ANSI-stripped, safe text) ---\n${formatRawTerminalSnapshotForError(raw)}`
  )
}

function stdoutLooksLikeInteractiveCliPtyCapture(stdout: string): boolean {
  if (stdout.includes('\x1b[2K')) return true
  const snippets = [
    'y or n; /stop to exit recall',
    'type your answer; /stop to exit recall',
    'y/N',
    'n/Y',
    '↑↓ Enter or number to select; Esc asks to leave recall (y/n confirm)',
    '↑↓ Enter to select; other keys cancel',
  ] as const
  if (snippets.some((s) => stdout.includes(s))) return true
  return stdout.includes('\x1b[') && stdout.includes('→')
}

function assertNonInteractiveCliOutput(stdout: string): void {
  if (stdoutLooksLikeInteractiveCliPtyCapture(stdout)) {
    failCliAssertion(WRONG_NON_INTERACTIVE_STEP, stdout)
  }
}

function assertSectionContainsSubstring(
  haystack: string,
  needle: string,
  sectionLabel: string
): void {
  if (haystack.includes(needle)) return
  failCliAssertion(
    `Expected "${needle}" in ${sectionLabel}. Content:\n${headPreview(haystack)}`,
    haystack
  )
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return cy.get<string>(OUTPUT_ALIAS).then((stdout) => {
        assertNonInteractiveCliOutput(stdout)
        assertSectionContainsSubstring(stdout, expected, SECTION.nonInteractive)
      }) as unknown as Cypress.Chainable<void>
    },
  }
}

export { nonInteractiveOutput }
