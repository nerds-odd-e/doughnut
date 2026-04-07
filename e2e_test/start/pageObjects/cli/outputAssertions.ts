import { formatRawTerminalSnapshotForError } from 'tty-assert/errorSnapshotFormatting'
import {
  TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS,
  waitForTextInSurface,
} from 'tty-assert/waitForTextInSurface'

/** Matches `waitForTextInSurface` poll cadence when the library polls internally; Cypress re-reads the PTY buffer on each attempt at this interval. */
const CLI_OUTPUT_ASSERT_RETRY_MS = TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
const CLI_OUTPUT_ASSERT_TIMEOUT_MS = 3000

function failCliAssertion(message: string, raw: string): never {
  throw new Error(
    `${message}\n\n--- CLI terminal snapshot (ANSI-stripped, safe text) ---\n${formatRawTerminalSnapshotForError(raw)}`
  )
}

function escapeRegExpLiteral(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function retryInteractiveAssertion(
  assert: (raw: string) => void | Promise<void>,
  screenshotName: string,
  timeoutMs: number = CLI_OUTPUT_ASSERT_TIMEOUT_MS
): Cypress.Chainable<void> {
  const tryOnce = (deadline: number): Cypress.Chainable<void> => {
    return cy.task<string>('cliInteractivePtyGetBuffer').then((raw) => {
      return Cypress.Promise.resolve(assert(raw)).then(
        () => undefined,
        (err: unknown) => {
          const lastError = err instanceof Error ? err : new Error(String(err))
          if (Date.now() >= deadline) {
            return cy.screenshot(screenshotName).then(() => {
              throw lastError
            })
          }
          return cy
            .wait(CLI_OUTPUT_ASSERT_RETRY_MS)
            .then(() => tryOnce(deadline))
        }
      )
    }) as Cypress.Chainable<void>
  }
  return cy.wrap(null).then(() => tryOnce(Date.now() + timeoutMs))
}

async function assertStrippedPtyTranscriptContains(
  raw: string,
  expected: string,
  domainHeading: string
): Promise<void> {
  if (expected === '') return
  await waitForTextInSurface({
    raw,
    needle: expected,
    surface: 'strippedTranscript',
    timeoutMs: 0,
    retryMs: CLI_OUTPUT_ASSERT_RETRY_MS,
    strict: false,
    messagePrefix: `${domainHeading}.`,
  })
}

async function assertAnsweredQuestionsContains(
  raw: string,
  expected: string
): Promise<void> {
  await assertStrippedPtyTranscriptContains(
    raw,
    expected,
    'Answered questions (in answered questions)'
  )
}

/** Gray background from chalk `bgGray` / bright black (past user message block). */
const PAST_USER_MSG_GRAY_BG_SGR = '\x1b[100m'
/** Gray foreground from chalk `grey` — not sufficient for a past user message block. */
const GRAY_FG_ONLY_SGR = '\x1b[90m'

/** Anchors for the guidance region, tried in priority order (most specific first). */
const GUIDANCE_ANCHORS: RegExp[] = [/^\s*└/, /^\s*>\s*$/, /> /]
const GUIDANCE_FALLBACK_ROWS = 8

async function assertCurrentGuidanceContains(
  raw: string,
  expected: string
): Promise<void> {
  if (expected === '') return
  await waitForTextInSurface({
    raw,
    needle: expected,
    surface: 'viewableBuffer',
    startAfterAnchor: GUIDANCE_ANCHORS,
    fallbackRowCount: GUIDANCE_FALLBACK_ROWS,
    timeoutMs: 0,
    strict: false,
    messagePrefix: 'Current guidance assertion failed.',
  })
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
  const tryOnce = () => {
    return cy.task<string>('cliInteractivePtyGetBuffer').then((raw) => {
      return Cypress.Promise.resolve(
        assertCurrentGuidanceContains(raw, prompt)
      ).then(
        () => onReady(),
        (err: unknown) => {
          const lastError = err instanceof Error ? err : new Error(String(err))
          if (Date.now() >= deadline) {
            return cy.screenshot(screenshotName).then(() => {
              throw lastError
            })
          }
          return cy.wait(CLI_OUTPUT_ASSERT_RETRY_MS).then(() => tryOnce())
        }
      )
    }) as Cypress.Chainable<null>
  }
  return cy.wrap(null).then(() => tryOnce())
}

async function assertCurrentGuidanceContainsBold(
  raw: string,
  text: string
): Promise<void> {
  await waitForTextInSurface({
    raw,
    needle: text,
    surface: 'viewableBuffer',
    startAfterAnchor: GUIDANCE_ANCHORS,
    fallbackRowCount: GUIDANCE_FALLBACK_ROWS,
    timeoutMs: 0,
    retryMs: CLI_OUTPUT_ASSERT_RETRY_MS,
    strict: false,
    requireBold: true,
    messagePrefix: 'Current guidance (expectContainsBold).',
  })
}

function lastIndexOfExpectedOrFail(raw: string, expected: string): number {
  const lastIdx = raw.lastIndexOf(expected)
  if (lastIdx === -1) {
    failCliAssertion(
      `Past user message check: expected text ${JSON.stringify(expected)} not found in PTY buffer.`,
      raw
    )
  }
  return lastIdx
}

async function assertPastUserMessageContainsInFullBuffer(
  raw: string,
  expected: string
): Promise<void> {
  if (expected === '') return
  await waitForTextInSurface({
    raw,
    needle: expected,
    surface: 'fullBuffer',
    timeoutMs: 0,
    retryMs: CLI_OUTPUT_ASSERT_RETRY_MS,
    strict: false,
    messagePrefix: 'Past user messages (in past user messages, full buffer).',
  })
}

async function assertPastUserMessageBlankLineAboveInStrippedTranscript(
  raw: string,
  expected: string
): Promise<void> {
  if (expected === '') return
  await waitForTextInSurface({
    raw,
    needle: new RegExp(
      String.raw`(?:^|\n)[^\S\n]*\n[^\n]*${escapeRegExpLiteral(expected)}[^\n]*`
    ),
    surface: 'strippedTranscript',
    timeoutMs: 0,
    retryMs: CLI_OUTPUT_ASSERT_RETRY_MS,
    strict: false,
    messagePrefix:
      'Past user messages must leave one blank line above the matching user message.',
  })
}

function assertPastUserMessageNotGrayForegroundOnly(
  raw: string,
  expected: string
): void {
  if (expected === '') return
  const lastIdx = lastIndexOfExpectedOrFail(raw, expected)
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
}

function assertPastUserMessageGrayBackgroundBlock(
  raw: string,
  expected: string
): void {
  if (expected === '') return
  const lastIdx = lastIndexOfExpectedOrFail(raw, expected)
  const windowBefore = raw.slice(Math.max(0, lastIdx - 120), lastIdx)
  const hasGrayBg = windowBefore.includes(PAST_USER_MSG_GRAY_BG_SGR)

  if (!hasGrayBg) {
    failCliAssertion(
      `Past user message ${JSON.stringify(expected)} must appear in a gray-background block (expect ANSI ${JSON.stringify(PAST_USER_MSG_GRAY_BG_SGR)} immediately before the text). ` +
        `No gray-background SGR in the bytes before the last occurrence of that text.`,
      raw
    )
  }
}

function pastCliAssistantMessages() {
  return {
    expectContains(
      expected: string,
      options?: { timeoutMs?: number }
    ): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) =>
          assertStrippedPtyTranscriptContains(
            raw,
            expected,
            'Past CLI assistant messages (in past CLI assistant messages)'
          ),
        'cli-interactive-pty-past-assistant-assertion',
        options?.timeoutMs
      )
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertAnsweredQuestionsContains(raw, expected),
        'cli-interactive-pty-answered-questions-assertion'
      )
    },
  }
}

function pastUserMessages() {
  return {
    expectContainsInFullBuffer(expected: string): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertPastUserMessageContainsInFullBuffer(raw, expected),
        'cli-interactive-pty-past-user-full-buffer'
      )
    },
    expectBlankLineAboveInStrippedTranscript(
      expected: string
    ): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) =>
          assertPastUserMessageBlankLineAboveInStrippedTranscript(
            raw,
            expected
          ),
        'cli-interactive-pty-past-user-blank-line'
      )
    },
    expectNotGrayForegroundOnlyWithoutBackground(
      expected: string
    ): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertPastUserMessageNotGrayForegroundOnly(raw, expected),
        'cli-interactive-pty-past-user-not-fg-only'
      )
    },
    expectGrayBackgroundBlock(expected: string): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertPastUserMessageGrayBackgroundBlock(raw, expected),
        'cli-interactive-pty-past-user-gray-bg'
      )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertCurrentGuidanceContains(raw, expected),
        'cli-interactive-pty-current-guidance-assertion'
      )
    },
    expectContainsBold(text: string): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertCurrentGuidanceContainsBold(raw, text),
        'cli-interactive-pty-current-guidance-bold-assertion'
      )
    },
  }
}

export {
  answeredQuestions,
  currentGuidance,
  pastCliAssistantMessages,
  pastUserMessages,
}
