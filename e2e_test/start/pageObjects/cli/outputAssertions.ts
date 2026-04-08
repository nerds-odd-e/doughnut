import type { CliInteractiveAssertRequest } from '../../../config/cliInteractiveAssertRequest'
import {
  currentGuidanceContainsAssertRequest,
  currentGuidanceContainsBoldAssertRequest,
  waitForCurrentGuidancePromptAssertRequest,
} from './currentGuidanceCliAssertRequests'
import {
  TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS,
  waitForTextInSurface,
} from 'tty-assert/waitForTextInSurface'

/** Matches `waitForTextInSurface` poll cadence when the library polls internally; Cypress re-reads the PTY buffer on each attempt at this interval. */
const CLI_OUTPUT_ASSERT_RETRY_MS = TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
const CLI_OUTPUT_ASSERT_TIMEOUT_MS = 3000

/** Cypress task wall clock: must exceed managed-session `timeoutMs` used by Current guidance asserts. */
const CLI_INTERACTIVE_ASSERT_TASK_TIMEOUT_MS = 15_000

function runCliInteractiveAssertWithScreenshot(
  body: CliInteractiveAssertRequest,
  screenshotName: string
): Cypress.Chainable<void> {
  return cy.wrap(null, { log: false }).then((_subject: null) => {
    return new Cypress.Promise<void>((resolve, reject) => {
      const taskChain = cy.task('cliInteractiveAssert', body, {
        timeout: CLI_INTERACTIVE_ASSERT_TASK_TIMEOUT_MS,
      }) as Cypress.Chainable<null> & {
        then(
          onFulfilled: (value: null) => void,
          onRejected: (err: unknown) => void
        ): Cypress.Chainable<unknown>
      }
      taskChain.then(
        () => {
          resolve()
        },
        (err: unknown) => {
          cy.screenshot(screenshotName).then(() => {
            reject(err)
          })
        }
      )
    })
  })
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

/**
 * Waits until Current guidance contains `prompt`, then runs `onReady` (e.g. PTY write).
 * Assertion and retry run in the plugin via `cliInteractiveAssert` (managed PTY session).
 */
export function whenCurrentGuidanceContainsThen(
  prompt: string,
  onReady: () => Cypress.Chainable<null>,
  screenshotName: string
): Cypress.Chainable<null> {
  if (prompt === '') return onReady()
  return runCliInteractiveAssertWithScreenshot(
    waitForCurrentGuidancePromptAssertRequest(prompt),
    screenshotName
  ).then(() => onReady()) as Cypress.Chainable<null>
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

/** Full-buffer presence plus gray-block SGR rules in one `waitForTextInSurface` (single xterm gray pass). */
async function assertPastUserMessageInFullBufferWithGrayBlock(
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
    requireGrayBackgroundBlock: true,
    messagePrefix:
      'Past user messages (full buffer + gray background block, no fg-only gray).',
  })
}

async function assertPastUserMessagesAll(raw: string, expected: string) {
  await assertPastUserMessageInFullBufferWithGrayBlock(raw, expected)
  await assertPastUserMessageBlankLineAboveInStrippedTranscript(raw, expected)
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
    /**
     * One Cypress retry loop: full-buffer text + gray-block styling, then blank line above in the
     * stripped transcript (two `waitForTextInSurface` calls per attempt).
     */
    expectDisplayed(expected: string): Cypress.Chainable<void> {
      return retryInteractiveAssertion(
        (raw) => assertPastUserMessagesAll(raw, expected),
        'cli-interactive-pty-past-user-displayed'
      )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<void> {
      if (expected === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        currentGuidanceContainsAssertRequest(expected),
        'cli-interactive-pty-current-guidance-assertion'
      )
    },
    expectContainsBold(text: string): Cypress.Chainable<void> {
      if (text === '')
        return cy.wrap(null, {
          log: false,
        }) as unknown as Cypress.Chainable<void>
      return runCliInteractiveAssertWithScreenshot(
        currentGuidanceContainsBoldAssertRequest(text),
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
