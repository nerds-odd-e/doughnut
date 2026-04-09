import { Terminal } from '@xterm/headless'
import {
  formatFinalViewportPlaintextForError,
  formatRawTerminalSnapshotForError,
} from './errorSnapshotFormatting'
import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'
import {
  disposeBufferedPtySession,
  startBufferedPtySession,
  type BufferedPtySession,
  type StartBufferedPtySessionOptions,
} from './ptySession'
import { validateAndResolveCellExpectations } from './cellExpectations'
import { viewportPlaintextFromHeadlessTerminal } from './ptyTranscriptToVisiblePlaintextViaXterm'
import { viewportPngBuffersToGif } from './viewportPngSequenceToGif'
import { viewportPngFromHeadlessTerminal } from './viewportPngFromHeadlessTerminal'
import {
  attemptOnceOnLiveTerminal,
  attemptOnceStrippedTranscript,
  writeTranscriptToTerminal,
} from './surfaceAttemptOnTerminal'
import { TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS } from './locatorRetryMs'
import {
  buildTtyAssertDumpDiagnostics,
  type TtyAssertDumpDiagnostics,
} from './ttyAssertDumpDiagnostics'
import { pollSurfaceAssertLoop } from './pollSurfaceAssertLoop'
import type { WaitForTextInSurfaceOptions } from './waitForTextInSurface'

export type ManagedTtyAssertOptions = Omit<WaitForTextInSurfaceOptions, 'raw'>

/** Plain object form of `RegExp` for JSON-serializable assert payloads (e.g. `cy.task`). */
export type JsonRegexp = { source: string; flags?: string }

export type ManagedTtyAssertInput = Omit<
  ManagedTtyAssertOptions,
  'needle' | 'startAfterAnchor'
> & {
  needle: string | RegExp | JsonRegexp
  startAfterAnchor?: (RegExp | JsonRegexp)[]
}

function regExpFromJsonRegexp(r: JsonRegexp): RegExp {
  return new RegExp(r.source, r.flags ?? '')
}

export function normalizeManagedTtyAssertInput(
  input: ManagedTtyAssertInput
): ManagedTtyAssertOptions {
  const needle =
    typeof input.needle === 'string' || input.needle instanceof RegExp
      ? input.needle
      : regExpFromJsonRegexp(input.needle)
  const startAfterAnchor = input.startAfterAnchor?.map((a) =>
    a instanceof RegExp ? a : regExpFromJsonRegexp(a)
  )
  return { ...input, needle, startAfterAnchor }
}

/** Wired by {@link startManagedTtySession} so PTY `onData` can schedule viewport animation samples. */
export type ManagedTtySessionPtyDataBridge = {
  onPtyData: () => void
}

const VIEWPORT_ANIM_DEBOUNCE_MS = 40
const VIEWPORT_ANIM_MAX_FRAMES = 56

const CLI_TERMINAL_RAW_SNAPSHOT_HEADING =
  '--- CLI terminal snapshot (ANSI-stripped, safe text) ---'

function appendRawTerminalSnapshotForErrorMessage(
  body: string,
  raw: string
): string {
  return `${body}\n\n${CLI_TERMINAL_RAW_SNAPSHOT_HEADING}\n${formatRawTerminalSnapshotForError(raw)}`
}

function appendManagedAssertFailureDiagnostics(
  body: string,
  raw: string,
  term: Terminal
): string {
  const viewportPlain = viewportPlaintextFromHeadlessTerminal(term)
  const withViewport = `${body}\n\n${formatFinalViewportPlaintextForError(viewportPlain)}`
  return appendRawTerminalSnapshotForErrorMessage(withViewport, raw)
}

export type ManagedTtySession = {
  readonly session: BufferedPtySession
  write(data: string): void
  submit(line: string): void
  assert(opts: ManagedTtyAssertInput): Promise<void>
  captureViewportPng(): Promise<Buffer>
  /** Recorded viewport PNGs when a {@link ManagedTtySessionPtyDataBridge} was used; else empty. */
  getViewportAnimationPngs(): Buffer[]
  /** Flushes pending animation sample, then builds a GIF from recorded frames (needs ≥2 frames). */
  buildViewportAnimationGif(): Promise<Buffer>
  dumpDiagnostics(): Promise<TtyAssertDumpDiagnostics>
  dispose(): void
}

export function attachManagedTtySession(
  session: BufferedPtySession,
  geometry?: { cols?: number; rows?: number },
  ptyDataBridge?: ManagedTtySessionPtyDataBridge
): ManagedTtySession {
  const cols = geometry?.cols ?? CLI_INTERACTIVE_PTY_COLS
  const rows = geometry?.rows ?? CLI_INTERACTIVE_PTY_ROWS
  const term = new Terminal({
    cols,
    rows,
    allowProposedApi: true,
  })
  let replayedByteCount = 0
  let disposed = false

  const animFrames: Buffer[] = []
  let lastAnimViewportPlain: string | undefined
  let animDebounceTimer: ReturnType<typeof setTimeout> | undefined

  async function syncReplay(): Promise<void> {
    if (disposed) return
    const full = session.buf.text
    if (full.length < replayedByteCount) {
      term.reset()
      await writeTranscriptToTerminal(term, full)
      replayedByteCount = full.length
      return
    }
    const delta = full.slice(replayedByteCount)
    if (delta.length === 0) return
    const targetEnd = full.length
    await writeTranscriptToTerminal(term, delta)
    replayedByteCount = targetEnd
  }

  async function flushAnimationFrame(): Promise<void> {
    if (disposed || !ptyDataBridge) return
    await syncReplay()
    const plain = viewportPlaintextFromHeadlessTerminal(term)
    if (plain === lastAnimViewportPlain) return
    lastAnimViewportPlain = plain
    animFrames.push(viewportPngFromHeadlessTerminal(term))
    while (animFrames.length > VIEWPORT_ANIM_MAX_FRAMES) {
      animFrames.shift()
    }
  }

  function scheduleAnimationSample(): void {
    if (disposed || !ptyDataBridge) return
    clearTimeout(animDebounceTimer)
    animDebounceTimer = setTimeout(() => {
      flushAnimationFrame().catch(() => {
        /* best-effort viewport sample; ignore PTY/xterm errors */
      })
    }, VIEWPORT_ANIM_DEBOUNCE_MS)
  }

  if (ptyDataBridge) {
    ptyDataBridge.onPtyData = scheduleAnimationSample
  }

  async function flushPendingAnimationSample(): Promise<void> {
    if (!ptyDataBridge) return
    clearTimeout(animDebounceTimer)
    animDebounceTimer = undefined
    await flushAnimationFrame()
  }

  return {
    session,
    write(data: string) {
      session.pty.write(data)
    },
    submit(line: string) {
      session.pty.write(`${line}\r`)
    },
    async assert(opts: ManagedTtyAssertInput): Promise<void> {
      if (disposed) {
        throw new Error('ManagedTtySession.assert after dispose')
      }

      const normalized = normalizeManagedTtyAssertInput(opts)

      const cellExpectations = validateAndResolveCellExpectations({
        surface: normalized.surface,
        needle: normalized.needle,
        cellExpectations: normalized.cellExpectations,
      })

      const timeoutMs = normalized.timeoutMs ?? 3000
      const retryMs = normalized.retryMs ?? TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
      const strict = normalized.strict ?? true
      const messagePrefix = normalized.messagePrefix

      await pollSurfaceAssertLoop({
        surface: normalized.surface,
        timeoutMs,
        retryMs,
        messagePrefix,
        runAttempt: async () => {
          if (disposed) {
            throw new Error('ManagedTtySession.assert after dispose')
          }
          await syncReplay()
          const raw = session.buf.text
          const result =
            normalized.surface === 'strippedTranscript'
              ? attemptOnceStrippedTranscript({
                  needle: normalized.needle,
                  surface: 'strippedTranscript',
                  raw,
                  strict,
                  cols,
                  rows,
                })
              : attemptOnceOnLiveTerminal(term, {
                  needle: normalized.needle,
                  surface: normalized.surface,
                  raw,
                  strict,
                  cols,
                  rows,
                  startAfterAnchor: normalized.startAfterAnchor,
                  fallbackRowCount: normalized.fallbackRowCount,
                  cellExpectations,
                })
          return { raw, result }
        },
        appendFailure: (body, raw) =>
          appendManagedAssertFailureDiagnostics(body, raw, term),
      })
    },
    async captureViewportPng(): Promise<Buffer> {
      if (disposed) {
        throw new Error('ManagedTtySession.captureViewportPng after dispose')
      }
      await syncReplay()
      return viewportPngFromHeadlessTerminal(term)
    },
    getViewportAnimationPngs(): Buffer[] {
      return [...animFrames]
    },
    async buildViewportAnimationGif(): Promise<Buffer> {
      if (!ptyDataBridge) {
        throw new Error(
          'ManagedTtySession.buildViewportAnimationGif: session was started without PTY animation bridge'
        )
      }
      if (disposed) {
        throw new Error(
          'ManagedTtySession.buildViewportAnimationGif after dispose'
        )
      }
      await flushPendingAnimationSample()
      if (animFrames.length < 2) {
        throw new Error(
          `ManagedTtySession.buildViewportAnimationGif: need at least 2 distinct viewport frames, got ${animFrames.length}`
        )
      }
      return viewportPngBuffersToGif([...animFrames])
    },
    async dumpDiagnostics(): Promise<TtyAssertDumpDiagnostics> {
      if (disposed) {
        throw new Error('ManagedTtySession.dumpDiagnostics after dispose')
      }
      await syncReplay()
      const raw = session.buf.text
      const replayed = viewportPlaintextFromHeadlessTerminal(term)
      return buildTtyAssertDumpDiagnostics({ raw, replayedPlain: replayed })
    },
    dispose() {
      if (disposed) return
      disposed = true
      clearTimeout(animDebounceTimer)
      animDebounceTimer = undefined
      try {
        term.dispose()
      } catch {
        /* ignore */
      }
      disposeBufferedPtySession(session)
    },
  }
}

export async function startManagedTtySession(
  opts: StartBufferedPtySessionOptions,
  geometry?: { cols?: number; rows?: number }
): Promise<ManagedTtySession> {
  const cols = opts.cols ?? geometry?.cols ?? CLI_INTERACTIVE_PTY_COLS
  const rows = opts.rows ?? geometry?.rows ?? CLI_INTERACTIVE_PTY_ROWS
  const bridge: ManagedTtySessionPtyDataBridge = {
    onPtyData: () => undefined,
  }
  const session = await startBufferedPtySession({
    ...opts,
    cols,
    rows,
    onAfterPtyData: () => bridge.onPtyData(),
  })
  return attachManagedTtySession(session, { cols, rows }, bridge)
}
