import {
  TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS,
  waitForTextInSurface,
} from 'tty-assert/waitForTextInSurface'

/** Matches `waitForTextInSurface` poll cadence when the library polls internally; Cypress re-reads the PTY buffer on each attempt at this interval. */
const CLI_OUTPUT_ASSERT_RETRY_MS = TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
const CLI_OUTPUT_ASSERT_TIMEOUT_MS = 3000

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

async function assertPastUserMessageNotGrayForegroundOnly(
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
    rejectGrayForegroundOnlyWithoutGrayBackground: true,
    messagePrefix:
      'Past user messages (gray foreground only without background).',
  })
}

async function assertPastUserMessageGrayBackgroundBlock(
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
    requireGrayBackgroundBlock: true,
    messagePrefix: 'Past user messages (gray background block).',
  })
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
