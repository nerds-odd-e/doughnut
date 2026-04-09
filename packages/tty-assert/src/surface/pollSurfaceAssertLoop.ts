import { formatSearchSurfaceFailure } from '../diagnostics/errorSnapshotFormatting'
import type { SurfaceAttemptResult } from '../xterm/surfaceAttemptOnTerminal'
import { TtyAssertStrictModeViolationError } from './ttyAssertStrictModeError'

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function withOptionalMessagePrefix(
  prefix: string | undefined,
  body: string
): string {
  if (prefix == null || prefix === '') return body
  const p = prefix.replace(/\n+$/, '')
  return `${p}\n${body}`
}

type TtySurface = 'viewableBuffer' | 'fullBuffer' | 'strippedTranscript'

/**
 * Shared poll / timeout / strict handling for surface text asserts.
 * `runAttempt` supplies fresh `raw` and the result of one {@link attemptOnce}-style check.
 */
export async function pollSurfaceAssertLoop(opts: {
  surface: TtySurface
  timeoutMs: number
  retryMs: number
  messagePrefix?: string
  runAttempt: () => Promise<{ raw: string; result: SurfaceAttemptResult }>
  appendFailure: (body: string, raw: string) => string
}): Promise<void> {
  const {
    surface,
    timeoutMs,
    retryMs,
    messagePrefix,
    runAttempt,
    appendFailure,
  } = opts
  const started = Date.now()
  let lastFail: { snapshot: string; detail: string } | undefined

  for (;;) {
    const { raw, result } = await runAttempt()

    if (result.ok === true) return

    if (result.ok === 'strict') {
      const body = withOptionalMessagePrefix(
        messagePrefix,
        formatSearchSurfaceFailure(surface, result.message, result.snapshot)
      )
      throw new TtyAssertStrictModeViolationError(appendFailure(body, raw))
    }

    lastFail = { snapshot: result.snapshot, detail: result.detail }
    if (Date.now() - started >= timeoutMs) {
      const detail =
        timeoutMs === 0
          ? lastFail.detail
          : `Timeout after ${timeoutMs}ms. ${lastFail.detail}`
      const body = withOptionalMessagePrefix(
        messagePrefix,
        formatSearchSurfaceFailure(surface, detail, lastFail.snapshot)
      )
      throw new Error(appendFailure(body, raw))
    }
    await sleep(retryMs)
  }
}
