/**
 * Hub for Cypress assertions on **terminal-visible** CLI output.
 *
 * Add new checks here (or in `tty-assert-staging` / `cliPtyCurrentGuidanceFromReplay`) so
 * failures stay consistent: bounded retries, then an error that includes an
 * ANSI-stripped snapshot of the buffer, and a Cypress screenshot on the final
 * failure path where this module throws.
 *
 * **Generic (imported):** `tty-assert-staging/errorSnapshotFormatting` (truncation, safe visible text
 * for errors), `stripAnsi`, `ptyTranscriptToVisiblePlaintext`; plus `extractCurrentGuidanceFromReplayedPlaintext`
 * from `cliPtyCurrentGuidanceFromReplay` (Ink-shaped guidance region — Doughnut-specific heuristic, not Cypress).
 *
 * **Doughnut-specific (this file):** non-interactive vs PTY sniffing (`stdoutLooksLikeInteractiveCliPtyCapture`,
 * recall-hint substrings), past user gray-background SGR checks, guidance assertion copy, `cy.task` reads,
 * retry timing, and screenshot names.
 *
 * Surfaces:
 * - **Non-interactive**: one-shot stdout from installed `version` / `update` (alias `@doughnutOutput`).
 * - **Interactive PTY**: live buffer from plugin task `cliInteractivePtyGetBuffer` (`interactiveCliPtySession`).
 */
import { extractCurrentGuidanceFromReplayedPlaintext } from '../../../config/cliPtyCurrentGuidanceFromReplay'
import {
  formatRawTerminalSnapshotForError,
  headPreview,
  tailPreview,
  TERMINAL_ERROR_PREVIEW_LEN,
} from '../../../config/tty-assert-staging/errorSnapshotFormatting'
import { ptyTranscriptToVisiblePlaintext } from '../../../config/tty-assert-staging/ptyTranscriptToVisiblePlaintext'
import { stripAnsiCliPty } from '../../../config/tty-assert-staging/stripAnsi'

export const OUTPUT_ALIAS = '@doughnutOutput'

const SECTION = {
  nonInteractive: 'non-interactive output',
} as const

const CLI_OUTPUT_ASSERT_RETRY_MS = 50
const CLI_OUTPUT_ASSERT_TIMEOUT_MS = 3000

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

/** Re-read value each attempt; retry until assert passes or timeout, then screenshot and throw. */
function retryCliOutputAssertion(
  readRaw: () => Cypress.Chainable<string>,
  assert: (raw: string) => void,
  screenshotName: string
): void {
  const tryOnce = (deadline: number) => {
    readRaw().then((raw) => {
      let lastError: Error | undefined
      try {
        assert(raw)
        return
      } catch (err) {
        lastError = err instanceof Error ? err : new Error(String(err))
      }
      if (Date.now() >= deadline) {
        cy.screenshot(screenshotName).then(() => {
          throw lastError
        })
        return
      }
      cy.wait(CLI_OUTPUT_ASSERT_RETRY_MS).then(() => tryOnce(deadline))
    })
  }
  cy.wrap(null).then(() => {
    tryOnce(Date.now() + CLI_OUTPUT_ASSERT_TIMEOUT_MS)
  })
}

function retryInteractiveAssertion(
  assert: (raw: string) => void,
  screenshotName: string
): void {
  retryCliOutputAssertion(
    () => cy.task<string>('cliInteractivePtyGetBuffer'),
    assert,
    screenshotName
  )
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      retryCliOutputAssertion(
        () => cy.get<string>(OUTPUT_ALIAS),
        (stdout) => {
          assertNonInteractiveCliOutput(stdout)
          assertSectionContainsSubstring(
            stdout,
            expected,
            SECTION.nonInteractive
          )
        },
        'cli-non-interactive-output-assertion'
      )
    },
  }
}

const PAST_CLI_ASSISTANT_SECTION =
  'past CLI assistant messages (ANSI-stripped transcript; assistant lines, not gray user blocks)'

const ANSWERED_QUESTIONS_SECTION =
  'answered questions (recall session answered lines; ANSI-stripped transcript)'

function assertStrippedPtyTranscriptContains(
  raw: string,
  expected: string,
  sectionLabel: string
): void {
  const stripped = stripAnsiCliPty(raw)
  if (stripped.length === 0) {
    failCliAssertion(
      `Expected ${JSON.stringify(expected)} in ${sectionLabel}, but the PTY transcript is empty after stripping ANSI escape codes.`,
      raw
    )
  }
  if (stripped.includes(expected)) return
  failCliAssertion(
    `Expected substring in ${sectionLabel}.\n` +
      `  Expected: ${JSON.stringify(expected)}\n` +
      `  Transcript length: ${stripped.length}\n` +
      `  Head preview:\n${headPreview(stripped)}\n` +
      `  Tail preview:\n${tailPreview(stripped)}`,
    raw
  )
}

function assertPastCliAssistantMessagesContains(
  raw: string,
  expected: string
): void {
  assertStrippedPtyTranscriptContains(raw, expected, PAST_CLI_ASSISTANT_SECTION)
}

function assertAnsweredQuestionsContains(raw: string, expected: string): void {
  assertStrippedPtyTranscriptContains(raw, expected, ANSWERED_QUESTIONS_SECTION)
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
  const preview = headPreview(stripped)

  if (!stripped.includes(expected)) {
    failCliAssertion(
      `Past user messages: expected text ${JSON.stringify(expected)} in the transcript (ANSI-stripped).\n` +
        `  Transcript length: ${stripped.length}\n` +
        `  Preview:\n${preview}`,
      raw
    )
  }

  const lastIdx = raw.lastIndexOf(expected)
  const windowBefore = raw.slice(Math.max(0, lastIdx - 120), lastIdx)
  const hasGrayBg = windowBefore.includes(PAST_USER_MSG_GRAY_BG_SGR)
  const hasGrayFgOnly = windowBefore.includes(GRAY_FG_ONLY_SGR) && !hasGrayBg

  if (hasGrayFgOnly) {
    failCliAssertion(
      `Past user message ${JSON.stringify(expected)} must appear in a gray-background block in the past message area (ANSI ${JSON.stringify(PAST_USER_MSG_GRAY_BG_SGR)} before the text in the final paint). ` +
        `Found gray foreground only (${JSON.stringify(GRAY_FG_ONLY_SGR)}), which is not a gray-background block.`,
      raw
    )
  }

  if (!hasGrayBg) {
    failCliAssertion(
      `Past user message ${JSON.stringify(expected)} must appear in a gray-background block (expect ANSI ${JSON.stringify(PAST_USER_MSG_GRAY_BG_SGR)} immediately before the text). ` +
        `No gray-background SGR in the bytes before the last occurrence of that text.`,
      raw
    )
  }

  const normalized = stripped.replace(/\r/g, '')
  const lines = normalized.split('\n')
  for (let i = lines.length - 1; i >= 0; i--) {
    const line = lines[i]
    if (line === undefined || !line.includes(expected)) continue
    if (i === 0) {
      failCliAssertion(
        `Past user message line containing ${JSON.stringify(expected)} must have one blank line above it (top padding). ` +
          `It is the first line of the stripped transcript (no line above). Preview:\n${preview}`,
        raw
      )
    }
    const lineAbove = lines[i - 1]
    const prev = (lineAbove ?? '').trim()
    if (prev !== '') {
      failCliAssertion(
        `Past user message line containing ${JSON.stringify(expected)} must have one blank line above it (top padding in the past message area). ` +
          `The line above is not blank: ${JSON.stringify((lineAbove ?? '').slice(0, 200))}`,
        raw
      )
    }
    return
  }

  failCliAssertion(
    `Internal: stripped transcript includes ${JSON.stringify(expected)} but no line contained it when splitting on newlines.`,
    raw
  )
}

const CURRENT_GUIDANCE_SECTION = 'Current guidance (simulated visible screen)'

function escapeRegex(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

/** Bold start SGR (e.g. markdansi / chalk); optional other SGR between bold-on and text. */
function boldStyledTextPattern(text: string): RegExp {
  return new RegExp(`\\x1b\\[1m(?:\\x1b\\[[0-9;]*m)*${escapeRegex(text)}`)
}

function getGuidanceContext(raw: string): {
  replayedPlain: string
  guidancePlain: string
} {
  const replayedPlain = ptyTranscriptToVisiblePlaintext(raw)
  const guidancePlain =
    extractCurrentGuidanceFromReplayedPlaintext(replayedPlain)
  return { replayedPlain, guidancePlain }
}

function formatGuidanceRegion(
  guidancePlain: string,
  replayedPlain: string
): string {
  const guidanceSection =
    guidancePlain.length > TERMINAL_ERROR_PREVIEW_LEN
      ? `${guidancePlain.slice(-TERMINAL_ERROR_PREVIEW_LEN)}\n… (${guidancePlain.length} chars)`
      : guidancePlain || '(empty)'
  return (
    `  Guidance region (replayed plain):\n${guidanceSection}\n` +
    `  Tail preview of full replayed screen (plain):\n${tailPreview(replayedPlain)}`
  )
}

function assertCurrentGuidanceContains(raw: string, expected: string): void {
  const { replayedPlain, guidancePlain } = getGuidanceContext(raw)
  if (guidancePlain.includes(expected)) return
  failCliAssertion(
    `Expected substring in ${CURRENT_GUIDANCE_SECTION} (not raw PTY bytes).\n` +
      `  Expected: ${JSON.stringify(expected)}\n` +
      `  Guidance region (below boxed main prompt bottom ${JSON.stringify('└')} row if present, else below empty ${JSON.stringify('> ')} row or last line with that marker), replayed plain:\n` +
      formatGuidanceRegion(guidancePlain, replayedPlain),
    raw
  )
}

/**
 * Waits until Current guidance contains `prompt`, then runs `onReady` (e.g. PTY write).
 * Failures reuse assertCurrentGuidanceContains diagnostics (expected prompt vs visible guidance).
 */
export function whenCurrentGuidanceContainsThen(
  prompt: string,
  onReady: () => Cypress.Chainable<null>,
  screenshotName: string
): Cypress.Chainable<null> {
  const deadline = Date.now() + CLI_OUTPUT_ASSERT_TIMEOUT_MS
  const tryOnce = (): Cypress.Chainable<null> => {
    return cy.task<string>('cliInteractivePtyGetBuffer').then((raw) => {
      try {
        assertCurrentGuidanceContains(raw, prompt)
        return onReady()
      } catch (err) {
        const lastError = err instanceof Error ? err : new Error(String(err))
        if (Date.now() >= deadline) {
          return cy.screenshot(screenshotName).then(() => {
            throw lastError
          })
        }
        return cy.wait(CLI_OUTPUT_ASSERT_RETRY_MS).then(() => tryOnce())
      }
    })
  }
  return cy.wrap(null).then(() => tryOnce())
}

function assertCurrentGuidanceContainsBold(raw: string, text: string): void {
  const { replayedPlain, guidancePlain } = getGuidanceContext(raw)
  if (!guidancePlain.includes(text)) {
    failCliAssertion(
      `Expected ${JSON.stringify(text)} in ${CURRENT_GUIDANCE_SECTION}.\n` +
        formatGuidanceRegion(guidancePlain, replayedPlain),
      raw
    )
  }
  if (boldStyledTextPattern(text).test(raw)) return
  failCliAssertion(
    `Expected ${JSON.stringify(text)} with **bold** styling in ${CURRENT_GUIDANCE_SECTION}.\n` +
      `  Plain guidance includes the substring, but the PTY transcript did not contain a bold SGR sequence (e.g. \\x1b[1m) immediately before that text.\n` +
      `  (Recall layout / markdown rendering may be wrong, or text is not in the guidance region.)\n` +
      formatGuidanceRegion(guidancePlain, replayedPlain),
    raw
  )
}

function assertPastUserMessagesContains(raw: string, expected: string): void {
  const stripped = stripAnsiCliPty(raw)
  if (stripped.length === 0) {
    failCliAssertion(
      `Expected ${JSON.stringify(expected)} in past user messages, but the PTY transcript is empty after stripping ANSI escape codes.`,
      raw
    )
  }
  assertPastUserMessageBlock(raw, stripped, expected)
}

function pastCliAssistantMessages() {
  return {
    expectContains(expected: string) {
      retryInteractiveAssertion(
        (raw) => assertPastCliAssistantMessagesContains(raw, expected),
        'cli-interactive-pty-past-assistant-assertion'
      )
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string) {
      retryInteractiveAssertion(
        (raw) => assertAnsweredQuestionsContains(raw, expected),
        'cli-interactive-pty-answered-questions-assertion'
      )
    },
  }
}

function pastUserMessages() {
  return {
    expectContains(expected: string) {
      retryInteractiveAssertion(
        (raw) => assertPastUserMessagesContains(raw, expected),
        'cli-interactive-pty-past-user-assertion'
      )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string) {
      retryInteractiveAssertion(
        (raw) => assertCurrentGuidanceContains(raw, expected),
        'cli-interactive-pty-current-guidance-assertion'
      )
    },
    expectContainsBold(text: string) {
      retryInteractiveAssertion(
        (raw) => assertCurrentGuidanceContainsBold(raw, text),
        'cli-interactive-pty-current-guidance-bold-assertion'
      )
    },
  }
}

export {
  answeredQuestions,
  currentGuidance,
  nonInteractiveOutput,
  pastCliAssistantMessages,
  pastUserMessages,
}
