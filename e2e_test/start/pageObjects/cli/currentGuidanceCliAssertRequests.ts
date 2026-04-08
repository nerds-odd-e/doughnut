import type { CliInteractiveAssertRequest } from '../../../config/cliInteractiveAssertRequest'
import { TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS } from 'tty-assert/waitForTextInSurface'

const CURRENT_GUIDANCE_ASSERT_TIMEOUT_MS = 3000

/** Same anchors as legacy `GUIDANCE_ANCHORS` RegExps, JSON-serializable for `cliInteractiveAssert`. */
export const guidanceStartAfterAnchors = [
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

export function currentGuidanceContainsAssertRequest(
  expected: string
): CliInteractiveAssertRequest {
  return {
    ...guidanceBase,
    needle: { kind: 'text', value: expected },
    messagePrefix: 'Current guidance assertion failed.',
  }
}

export function currentGuidanceContainsBoldAssertRequest(
  text: string
): CliInteractiveAssertRequest {
  return {
    ...guidanceBase,
    needle: { kind: 'text', value: text },
    requireBold: true,
    messagePrefix: 'Current guidance (expectContainsBold).',
  }
}

export function waitForCurrentGuidancePromptAssertRequest(
  prompt: string
): CliInteractiveAssertRequest {
  return currentGuidanceContainsAssertRequest(prompt)
}
