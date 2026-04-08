import type { CliInteractiveAssertRequest } from '../../../config/cliInteractiveAssertRequest'
import { TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS } from 'tty-assert/waitForTextInSurface'

const CURRENT_GUIDANCE_ASSERT_TIMEOUT_MS = 3000
const TRANSCRIPT_ASSERT_TIMEOUT_MS = 3000

const guidanceStartAfterAnchors = [
  { source: '^\\s*└' },
  { source: '^\\s*>\\s*$' },
  { source: '> ' },
] as const

const guidanceBase: Pick<
  CliInteractiveAssertRequest,
  | 'surface'
  | 'timeoutMs'
  | 'retryMs'
  | 'strict'
  | 'fallbackRowCount'
  | 'startAfterAnchor'
> = {
  surface: 'viewableBuffer',
  timeoutMs: CURRENT_GUIDANCE_ASSERT_TIMEOUT_MS,
  retryMs: TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS,
  strict: false,
  fallbackRowCount: 8,
  startAfterAnchor: [...guidanceStartAfterAnchors],
}

const transcriptPollBase: Pick<
  CliInteractiveAssertRequest,
  'retryMs' | 'strict'
> = {
  retryMs: TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS,
  strict: false,
}

function escapeRegExpLiteral(text: string): string {
  return text.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

function currentGuidanceContainsAssertRequest(
  expected: string
): CliInteractiveAssertRequest {
  return {
    ...guidanceBase,
    needle: { kind: 'text', value: expected },
    messagePrefix: 'Current guidance assertion failed.',
  }
}

function currentGuidanceContainsBoldAssertRequest(
  text: string
): CliInteractiveAssertRequest {
  return {
    ...guidanceBase,
    needle: { kind: 'text', value: text },
    requireBold: true,
    messagePrefix: 'Current guidance (expectContainsBold).',
  }
}

function waitForCurrentGuidancePromptAssertRequest(
  prompt: string
): CliInteractiveAssertRequest {
  return currentGuidanceContainsAssertRequest(prompt)
}

function pastCliAssistantMessagesContainsAssertRequest(
  expected: string,
  timeoutMs?: number
): CliInteractiveAssertRequest {
  return {
    ...transcriptPollBase,
    needle: { kind: 'text', value: expected },
    surface: 'strippedTranscript',
    timeoutMs: timeoutMs ?? TRANSCRIPT_ASSERT_TIMEOUT_MS,
    messagePrefix:
      'Past CLI assistant messages (in past CLI assistant messages).',
  }
}

function answeredQuestionsContainsAssertRequest(
  expected: string
): CliInteractiveAssertRequest {
  return {
    ...transcriptPollBase,
    needle: { kind: 'text', value: expected },
    surface: 'strippedTranscript',
    timeoutMs: TRANSCRIPT_ASSERT_TIMEOUT_MS,
    messagePrefix: 'Answered questions (in answered questions).',
  }
}

function pastUserMessageFullBufferGrayAssertRequest(
  expected: string
): CliInteractiveAssertRequest {
  return {
    ...transcriptPollBase,
    needle: { kind: 'text', value: expected },
    surface: 'fullBuffer',
    timeoutMs: TRANSCRIPT_ASSERT_TIMEOUT_MS,
    rejectGrayForegroundOnlyWithoutGrayBackground: true,
    requireGrayBackgroundBlock: true,
    messagePrefix:
      'Past user messages (full buffer + gray background block, no fg-only gray).',
  }
}

function pastUserMessageBlankLineAboveAssertRequest(
  expected: string
): CliInteractiveAssertRequest {
  const source = String.raw`(?:^|\n)[^\S\n]*\n[^\n]*${escapeRegExpLiteral(expected)}[^\n]*`
  return {
    ...transcriptPollBase,
    needle: { kind: 'regex', source },
    surface: 'strippedTranscript',
    timeoutMs: TRANSCRIPT_ASSERT_TIMEOUT_MS,
    messagePrefix:
      'Past user messages must leave one blank line above the matching user message.',
  }
}

/**
 * Waits until Current guidance contains `prompt`, then runs `onReady` (e.g. PTY write).
 * Assertion and retry run in the plugin via `cliInteractiveAssert` (managed PTY session).
 */
export function whenCurrentGuidanceContainsThen(
  prompt: string,
  onReady: () => Cypress.Chainable<null>
): Cypress.Chainable<null> {
  if (prompt === '') return onReady()
  return cy
    .task<null>(
      'cliInteractiveAssert',
      waitForCurrentGuidancePromptAssertRequest(prompt)
    )
    .then(() => onReady())
}

function pastCliAssistantMessages() {
  return {
    expectContains(
      expected: string,
      options?: { timeoutMs?: number }
    ): Cypress.Chainable<null> {
      return cy.task<null>(
        'cliInteractiveAssert',
        pastCliAssistantMessagesContainsAssertRequest(
          expected,
          options?.timeoutMs
        )
      )
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string): Cypress.Chainable<null> {
      return cy.task<null>(
        'cliInteractiveAssert',
        answeredQuestionsContainsAssertRequest(expected)
      )
    },
  }
}

function pastUserMessages() {
  return {
    /**
     * Full-buffer text + gray-block styling, then blank line above in the stripped transcript
     * (two `cliInteractiveAssert` tasks; retry lives in the managed session per request).
     */
    expectDisplayed(expected: string): Cypress.Chainable<null> {
      return cy
        .task<null>(
          'cliInteractiveAssert',
          pastUserMessageFullBufferGrayAssertRequest(expected)
        )
        .then(() =>
          cy.task<null>(
            'cliInteractiveAssert',
            pastUserMessageBlankLineAboveAssertRequest(expected)
          )
        )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<null> {
      return cy.task<null>(
        'cliInteractiveAssert',
        currentGuidanceContainsAssertRequest(expected)
      )
    },
    expectContainsBold(text: string): Cypress.Chainable<null> {
      return cy.task<null>(
        'cliInteractiveAssert',
        currentGuidanceContainsBoldAssertRequest(text)
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
