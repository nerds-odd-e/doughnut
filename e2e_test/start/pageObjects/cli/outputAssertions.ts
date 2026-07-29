import type { ManagedTtyAssertInput } from 'tty-assert'
import { cliAssertTask } from './cliAssertTask'

const guidanceStartAfterAnchors = [
  { source: '^\\s*└' },
  { source: '^\\s*>\\s*$' },
  { source: '> ' },
] as const

const guidanceBase: Pick<
  ManagedTtyAssertInput,
  'surface' | 'strict' | 'fallbackRowCount' | 'startAfterAnchor'
> = {
  surface: 'viewableBuffer',
  strict: false,
  fallbackRowCount: 8,
  startAfterAnchor: [...guidanceStartAfterAnchors],
}

const transcriptPollBase: Pick<ManagedTtyAssertInput, 'strict'> = {
  strict: false,
}

function currentGuidanceContainsAssertRequest(
  expected: string,
  timeoutMs = 15_000
): ManagedTtyAssertInput {
  return {
    ...guidanceBase,
    needle: expected,
    messagePrefix: 'Current guidance assertion failed.',
    timeoutMs,
  }
}

function strippedTranscriptTextAssertRequest(
  expected: string,
  messagePrefix: string,
  timeoutMs = 15000
): ManagedTtyAssertInput {
  return {
    ...transcriptPollBase,
    needle: expected,
    surface: 'strippedTranscript',
    messagePrefix,
    timeoutMs,
  }
}

export function nonInteractiveCliOutputAssertRequest(
  expected: string
): ManagedTtyAssertInput {
  return {
    ...transcriptPollBase,
    needle: expected,
    surface: 'strippedTranscript',
    messagePrefix: 'Non-interactive CLI output.',
    timeoutMs: 0,
  }
}

function nonInteractiveOutput() {
  return {
    expectContains(expected: string) {
      return cliAssertTask(nonInteractiveCliOutputAssertRequest(expected))
    },
  }
}

/**
 * Waits until Current guidance contains `prompt`, then runs `onReady` (e.g. PTY write).
 * Assertion and retry run in the plugin via `cliAssert` (managed PTY session).
 */
export function whenCurrentGuidanceContainsThen(
  prompt: string,
  onReady: () => Cypress.Chainable<null>
): Cypress.Chainable<null> {
  if (prompt === '') return onReady()
  return cliAssertTask(currentGuidanceContainsAssertRequest(prompt)).then(() =>
    onReady()
  )
}

/**
 * Match consecutive lines in the transcript.
 *
 * The cumulative transcript is raw PTY output, so rows are separated by CRLF
 * and a plain multi-line needle never matches. Matching line by line with a
 * tolerant separator keeps the assertion about adjacency, which is the point
 * of asserting a block rather than each line on its own.
 */
function transcriptBlockPattern(expected: string): { source: string } {
  return {
    source: expected
      .split('\n')
      .map((line) => line.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'))
      .join('\\r?\\n'),
  }
}

function pastCliAssistantMessages() {
  return {
    expectContains(expected: string): Cypress.Chainable<null> {
      return cliAssertTask(
        strippedTranscriptTextAssertRequest(
          expected,
          'Past CLI assistant messages (in past CLI assistant messages).'
        )
      )
    },
    /** As `expectContains`, for an expectation spanning several lines. */
    expectContainsBlock(expected: string): Cypress.Chainable<null> {
      return cliAssertTask({
        ...transcriptPollBase,
        needle: transcriptBlockPattern(expected),
        surface: 'strippedTranscript',
        messagePrefix:
          'Past CLI assistant messages (consecutive lines in past CLI assistant messages).',
        timeoutMs: 15000,
      })
    },
  }
}

function answeredQuestions() {
  return {
    expectContains(expected: string): Cypress.Chainable<null> {
      return cliAssertTask(
        strippedTranscriptTextAssertRequest(
          expected,
          'Answered questions (in answered questions).'
        )
      )
    },
  }
}

function pastUserMessages() {
  return {
    /**
     * Full-buffer text + gray background (palette 8) on the matched span, then blank line above
     * in the stripped transcript (two `cliAssert` tasks; retry in managed session).
     */
    expectDisplayed(expected: string): Cypress.Chainable<null> {
      const escaped = expected.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
      const blankLineAboveSource = String.raw`(?:^|\n)[^\S\n]*\n[^\n]*${escaped}[^\n]*`
      return cliAssertTask({
        ...transcriptPollBase,
        needle: expected,
        surface: 'fullBuffer',
        cellExpectations: [
          {
            match: 'last',
            expectations: [{ kind: 'allBgPalette', index: 8 }],
          },
        ],
        messagePrefix:
          'Past user messages (full buffer + gray background block).',
      }).then(() =>
        cliAssertTask({
          ...transcriptPollBase,
          needle: { source: blankLineAboveSource },
          surface: 'strippedTranscript',
          messagePrefix:
            'Past user messages must leave one blank line above the matching user message.',
        })
      )
    },
  }
}

function currentGuidance() {
  return {
    expectContains(expected: string): Cypress.Chainable<null> {
      return cliAssertTask(currentGuidanceContainsAssertRequest(expected))
    },
    expectContainsBold(text: string): Cypress.Chainable<null> {
      return cliAssertTask({
        ...guidanceBase,
        needle: text,
        cellExpectations: [
          { match: 'first', expectations: [{ kind: 'allBold' }] },
        ],
        messagePrefix: 'Current guidance (expectContainsBold).',
      })
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
