import type { ManagedTtyAssertOptions } from 'tty-assert/managedTtySession'
import type { TtySearchSurface } from 'tty-assert/waitForTextInSurface'

export type CliInteractiveSerializedPattern =
  | { kind: 'text'; value: string }
  | { kind: 'regex'; source: string; flags?: string }

export type CliInteractiveSerializedAnchor = {
  source: string
  flags?: string
}

export type CliInteractiveAssertRequest = {
  needle: CliInteractiveSerializedPattern
  surface: TtySearchSurface
  timeoutMs?: number
  retryMs?: number
  strict?: boolean
  cols?: number
  rows?: number
  startAfterAnchor?: CliInteractiveSerializedAnchor[]
  fallbackRowCount?: number
  messagePrefix?: string
  requireBold?: boolean
  rejectGrayForegroundOnlyWithoutGrayBackground?: boolean
  requireGrayBackgroundBlock?: boolean
}

function patternToNeedle(p: CliInteractiveSerializedPattern): string | RegExp {
  if (p.kind === 'text') return p.value
  return new RegExp(p.source, p.flags ?? '')
}

function anchorsToRegExps(
  anchors: CliInteractiveSerializedAnchor[] | undefined
): RegExp[] | undefined {
  if (anchors == null) return undefined
  return anchors.map((a) => new RegExp(a.source, a.flags ?? ''))
}

export function cliInteractiveAssertRequestToManagedOptions(
  req: CliInteractiveAssertRequest
): ManagedTtyAssertOptions {
  const needle = patternToNeedle(req.needle)
  const startAfterAnchor = anchorsToRegExps(req.startAfterAnchor)

  return {
    needle,
    surface: req.surface,
    timeoutMs: req.timeoutMs,
    retryMs: req.retryMs,
    strict: req.strict,
    cols: req.cols,
    rows: req.rows,
    startAfterAnchor,
    fallbackRowCount: req.fallbackRowCount,
    messagePrefix: req.messagePrefix,
    requireBold: req.requireBold,
    rejectGrayForegroundOnlyWithoutGrayBackground:
      req.rejectGrayForegroundOnlyWithoutGrayBackground,
    requireGrayBackgroundBlock: req.requireGrayBackgroundBlock,
  }
}
