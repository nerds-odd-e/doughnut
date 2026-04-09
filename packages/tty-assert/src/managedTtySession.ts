import { Terminal } from '@xterm/headless'
import {
  TERMINAL_ERROR_RAW_TAIL_BYTES,
  formatFinalViewportPlaintextForError,
  formatRawTerminalSnapshotForError,
  formatSearchSurfaceFailure,
  headPreview,
  sanitizeVisibleTextForError,
  tailPreview,
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
import { stripAnsiCliPty } from './stripAnsi'
import {
  TtyAssertStrictModeViolationError,
  TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS,
  type WaitForTextInSurfaceOptions,
} from './waitForTextInSurface'

/** Same fields as `TtyAssertDumpFrames` from `./facade` (shared diagnostic shape). */
export type ManagedTtySessionDumpFrames = {
  rawByteLength: number
  ansiStrippedLength: number
  replayedScreenPlaintextHeadPreview: string
  replayedScreenPlaintextTailPreview: string
  strippedTranscriptTailPreview: string
  rawTailSanitizedPreview: string
}

export type ManagedTtyAssertOptions = Omit<WaitForTextInSurfaceOptions, 'raw'>

/** Wired by {@link startManagedTtySession} so PTY `onData` can schedule viewport animation samples. */
export type ManagedTtySessionPtyDataBridge = {
  onPtyData: () => void
}

const VIEWPORT_ANIM_DEBOUNCE_MS = 40
const VIEWPORT_ANIM_MAX_FRAMES = 56

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

const CLI_TERMINAL_RAW_SNAPSHOT_HEADING =
  '--- CLI terminal snapshot (ANSI-stripped, safe text) ---'

function withOptionalMessagePrefix(
  prefix: string | undefined,
  body: string
): string {
  if (prefix == null || prefix === '') return body
  const p = prefix.replace(/\n+$/, '')
  return `${p}\n${body}`
}

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
  assert(opts: ManagedTtyAssertOptions): Promise<void>
  captureViewportPng(): Promise<Buffer>
  /** Recorded viewport PNGs when a {@link ManagedTtySessionPtyDataBridge} was used; else empty. */
  getViewportAnimationPngs(): Buffer[]
  /** Flushes pending animation sample, then builds a GIF from recorded frames (needs ≥2 frames). */
  buildViewportAnimationGif(): Promise<Buffer>
  dumpFrames(): Promise<ManagedTtySessionDumpFrames>
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
    async assert(opts: ManagedTtyAssertOptions): Promise<void> {
      if (disposed) {
        throw new Error('ManagedTtySession.assert after dispose')
      }

      const cellExpectations = validateAndResolveCellExpectations({
        surface: opts.surface,
        needle: opts.needle,
        cellExpectations: opts.cellExpectations,
      })

      const timeoutMs = opts.timeoutMs ?? 3000
      const retryMs = opts.retryMs ?? TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
      const strict = opts.strict ?? true
      const messagePrefix = opts.messagePrefix

      const started = Date.now()
      let lastFail: { snapshot: string; detail: string } | undefined

      for (;;) {
        if (disposed) {
          throw new Error('ManagedTtySession.assert after dispose')
        }
        await syncReplay()
        const raw = session.buf.text

        let result:
          | { ok: true }
          | { ok: false; snapshot: string; detail: string }
          | { ok: 'strict'; message: string; snapshot: string }

        if (opts.surface === 'strippedTranscript') {
          result = attemptOnceStrippedTranscript({
            needle: opts.needle,
            surface: 'strippedTranscript',
            raw,
            strict,
            cols,
            rows,
          })
        } else {
          result = attemptOnceOnLiveTerminal(term, {
            needle: opts.needle,
            surface: opts.surface,
            raw,
            strict,
            cols,
            rows,
            startAfterAnchor: opts.startAfterAnchor,
            fallbackRowCount: opts.fallbackRowCount,
            cellExpectations,
          })
        }

        if (result.ok === true) return

        if (result.ok === 'strict') {
          const body = withOptionalMessagePrefix(
            messagePrefix,
            formatSearchSurfaceFailure(
              opts.surface,
              result.message,
              result.snapshot
            )
          )
          throw new TtyAssertStrictModeViolationError(
            appendManagedAssertFailureDiagnostics(body, raw, term)
          )
        }

        lastFail = { snapshot: result.snapshot, detail: result.detail }
        if (Date.now() - started >= timeoutMs) {
          const detail =
            timeoutMs === 0
              ? lastFail.detail
              : `Timeout after ${timeoutMs}ms. ${lastFail.detail}`
          const body = withOptionalMessagePrefix(
            messagePrefix,
            formatSearchSurfaceFailure(opts.surface, detail, lastFail.snapshot)
          )
          throw new Error(
            appendManagedAssertFailureDiagnostics(body, raw, term)
          )
        }
        await sleep(retryMs)
      }
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
    async dumpFrames(): Promise<ManagedTtySessionDumpFrames> {
      if (disposed) {
        throw new Error('ManagedTtySession.dumpFrames after dispose')
      }
      await syncReplay()
      const raw = session.buf.text
      const stripped = stripAnsiCliPty(raw)
      const replayed = viewportPlaintextFromHeadlessTerminal(term)
      return {
        rawByteLength: raw.length,
        ansiStrippedLength: stripped.length,
        replayedScreenPlaintextHeadPreview: headPreview(replayed),
        replayedScreenPlaintextTailPreview: tailPreview(replayed),
        strippedTranscriptTailPreview: tailPreview(stripped),
        rawTailSanitizedPreview: sanitizeVisibleTextForError(
          tailPreview(raw.slice(-TERMINAL_ERROR_RAW_TAIL_BYTES))
        ),
      }
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
  const bridge: ManagedTtySessionPtyDataBridge = {
    onPtyData: () => undefined,
  }
  const session = await startBufferedPtySession({
    ...opts,
    onAfterPtyData: () => bridge.onPtyData(),
  })
  return attachManagedTtySession(session, geometry, bridge)
}
