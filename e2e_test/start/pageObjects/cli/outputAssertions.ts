/**
 * Cucumber assertions on `@doughnutOutput`.
 *
 * - **Non-interactive**: one-shot CLI output (installed `version` / `update` spawns).
 * - **Interactive PTY**: `@cliInteractivePtyOutput` from `runInstalledCliInteractive`.
 */
export const OUTPUT_ALIAS = '@doughnutOutput'

export const CLI_INTERACTIVE_PTY_OUTPUT_ALIAS = '@cliInteractivePtyOutput'

/** Browser-safe ANSI strip for PTY transcripts (Cypress bundles page objects for the spec runner). */
function stripAnsi(text: string): string {
  const esc = `${String.fromCharCode(0x1b)}${String.fromCharCode(0x9b)}`
  const pattern = new RegExp(
    `[${esc}][[\\]()#;?]*(?:[0-9]{1,4}(?:;[0-9]{0,4})*)?[0-9A-ORZcf-nqry=><]`,
    'g'
  )
  return text.replace(pattern, '')
}

const SECTION = {
  nonInteractive: 'non-interactive output',
} as const

const CONTENT_PREVIEW_LEN = 500

const WRONG_NON_INTERACTIVE_STEP =
  'Expected non-interactive CLI output (e.g. `version` / `update` spawn), but this capture looks like an interactive PTY session.'

function stdoutLooksLikeInteractiveCliPtyCapture(stdout: string): boolean {
  if (stdout.includes('\x1b[2K')) return true
  const snippets = [
    'y or n; /stop to exit recall',
    'type your answer; /stop to exit recall',
    'y/N',
    'n/Y',
    '↑↓ Enter or number to select; Esc to cancel',
    '↑↓ Enter to select; other keys cancel',
  ] as const
  if (snippets.some((s) => stdout.includes(s))) return true
  return stdout.includes('\x1b[') && stdout.includes('→')
}

function expectSectionContainsSubstring(
  haystack: string,
  needle: string,
  sectionLabel: string
): void {
  const preview =
    haystack.length > CONTENT_PREVIEW_LEN
      ? `${haystack.slice(0, CONTENT_PREVIEW_LEN)}...`
      : haystack
  expect(
    haystack,
    `Expected "${needle}" in ${sectionLabel}. Content:\n${preview}`
  ).to.include(needle)
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      cy.get<string>(OUTPUT_ALIAS).then((stdout) => {
        expect(
          !stdoutLooksLikeInteractiveCliPtyCapture(stdout),
          WRONG_NON_INTERACTIVE_STEP
        ).to.be.true
        expectSectionContainsSubstring(stdout, expected, SECTION.nonInteractive)
      })
    },
  }
}

function pastCliAssistantMessages() {
  return {
    expectContains(expected: string) {
      cy.get<string>(CLI_INTERACTIVE_PTY_OUTPUT_ALIAS).then((raw) => {
        const stripped = stripAnsi(raw)
        if (stripped.length === 0) {
          expect(
            false,
            `Expected ${JSON.stringify(expected)} in past CLI assistant messages, but the PTY transcript is empty after stripping ANSI escape codes.`
          ).to.be.true
          return
        }
        const previewLen = 500
        const preview =
          stripped.length > previewLen
            ? `${stripped.slice(0, previewLen)}...`
            : stripped
        expect(
          stripped.includes(expected),
          `Expected substring in past CLI assistant messages (ANSI-stripped).\n` +
            `  Expected: ${JSON.stringify(expected)}\n` +
            `  Transcript length: ${stripped.length}\n` +
            `  Preview:\n${preview}`
        ).to.be.true
      })
    },
  }
}

export { nonInteractiveOutput, pastCliAssistantMessages }
