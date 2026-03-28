/**
 * Cucumber assertions on `@doughnutOutput`.
 *
 * - **Non-interactive**: one-shot CLI output (installed `version` / `update` spawns).
 * - **Interactive PTY**: `@cliInteractivePtyOutput` — use via `interactiveCli()` (plugin `interactiveCliPtySession`).
 */
import { stripAnsiCliPty } from '../../../config/cliPtyAnsi'

export const OUTPUT_ALIAS = '@doughnutOutput'

export const CLI_INTERACTIVE_PTY_OUTPUT_ALIAS = '@cliInteractivePtyOutput'

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
        const stripped = stripAnsiCliPty(raw)
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

/** Gray background from chalk `bgGray` / bright black (past user message block). */
const PAST_USER_MSG_GRAY_BG_SGR = '\x1b[100m'
/** Gray foreground from chalk `grey` — not sufficient for a past user message block. */
const GRAY_FG_ONLY_SGR = '\x1b[90m'

function assertPastUserMessageBlock(
  raw: string,
  stripped: string,
  expected: string
): void {
  const previewLen = 500
  const preview =
    stripped.length > previewLen
      ? `${stripped.slice(0, previewLen)}...`
      : stripped

  if (!stripped.includes(expected)) {
    expect(
      false,
      `Past user messages: expected text ${JSON.stringify(expected)} in the transcript (ANSI-stripped).\n` +
        `  Transcript length: ${stripped.length}\n` +
        `  Preview:\n${preview}`
    ).to.be.true
    return
  }

  const lastIdx = raw.lastIndexOf(expected)
  const windowBefore = raw.slice(Math.max(0, lastIdx - 120), lastIdx)
  const hasGrayBg = windowBefore.includes(PAST_USER_MSG_GRAY_BG_SGR)
  const hasGrayFgOnly = windowBefore.includes(GRAY_FG_ONLY_SGR) && !hasGrayBg

  if (hasGrayFgOnly) {
    expect(
      false,
      `Past user message ${JSON.stringify(expected)} must appear in a gray-background block in the past message area (ANSI ${JSON.stringify(PAST_USER_MSG_GRAY_BG_SGR)} before the text in the final paint). ` +
        `Found gray foreground only (${JSON.stringify(GRAY_FG_ONLY_SGR)}), which is not a gray-background block.`
    ).to.be.true
    return
  }

  if (!hasGrayBg) {
    expect(
      false,
      `Past user message ${JSON.stringify(expected)} must appear in a gray-background block (expect ANSI ${JSON.stringify(PAST_USER_MSG_GRAY_BG_SGR)} immediately before the text). ` +
        `No gray-background SGR in the bytes before the last occurrence of that text.`
    ).to.be.true
    return
  }

  const normalized = stripped.replace(/\r/g, '')
  const lines = normalized.split('\n')
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i]
    if (line === undefined || !line.includes(expected)) continue
    if (i === 0) {
      expect(
        false,
        `Past user message line containing ${JSON.stringify(expected)} must have one blank line above it (top padding). ` +
          `It is the first line of the stripped transcript (no line above). Preview:\n${preview}`
      ).to.be.true
      return
    }
    const lineAbove = lines[i - 1]
    const prev = (lineAbove ?? '').trim()
    if (prev !== '') {
      expect(
        false,
        `Past user message line containing ${JSON.stringify(expected)} must have one blank line above it (top padding in the past message area). ` +
          `The line above is not blank: ${JSON.stringify((lineAbove ?? '').slice(0, 200))}`
      ).to.be.true
      return
    }
    return
  }

  expect(
    false,
    `Internal: stripped transcript includes ${JSON.stringify(expected)} but no line contained it when splitting on newlines.`
  ).to.be.true
}

function pastUserMessages() {
  return {
    expectContains(expected: string) {
      cy.get<string>(CLI_INTERACTIVE_PTY_OUTPUT_ALIAS).then((raw) => {
        const stripped = stripAnsiCliPty(raw)
        if (stripped.length === 0) {
          expect(
            false,
            `Expected ${JSON.stringify(expected)} in past user messages, but the PTY transcript is empty after stripping ANSI escape codes.`
          ).to.be.true
          return
        }
        assertPastUserMessageBlock(raw, stripped, expected)
      })
    },
  }
}

export { nonInteractiveOutput, pastCliAssistantMessages, pastUserMessages }
