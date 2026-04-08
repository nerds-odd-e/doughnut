import type { CliInteractiveAssertRequest } from '../../../config/cliInteractiveAssertRequest'
import { TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS } from 'tty-assert/waitForTextInSurface'

const TRANSCRIPT_ASSERT_TIMEOUT_MS = 3000

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

export function pastCliAssistantMessagesContainsAssertRequest(
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

export function answeredQuestionsContainsAssertRequest(
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

export function pastUserMessageFullBufferGrayAssertRequest(
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

export function pastUserMessageBlankLineAboveAssertRequest(
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
