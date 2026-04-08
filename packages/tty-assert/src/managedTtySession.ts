import { Terminal } from '@xterm/headless'
import {
  formatRawTerminalSnapshotForError,
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
import { viewportPlaintextFromHeadlessTerminal } from './ptyTranscriptToVisiblePlaintextViaXterm'
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

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

function formatFailureMessage(
  surface: WaitForTextInSurfaceOptions['surface'],
  detail: string,
  snapshot: string
): string {
  const snap =
    snapshot.length > 8000
      ? `${snapshot.slice(0, 8000)}\n… (truncated)`
      : snapshot
  const note =
    surface === 'strippedTranscript'
      ? 'Search uses the ANSI-stripped cumulative transcript (same text as the snapshot below).'
      : 'Snapshot: newline-separated trimmed rows. Matching uses a flat row-major block with no newlines between rows.'
  return `${detail}\nSearch surface: "${surface}".\n${note}\n---\n${snap}\n---`
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

export type ManagedTtySession = {
  readonly session: BufferedPtySession
  write(data: string): void
  submit(line: string): void
  assert(opts: ManagedTtyAssertOptions): Promise<void>
  dumpFrames(): Promise<ManagedTtySessionDumpFrames>
  dispose(): void
}

export function attachManagedTtySession(
  session: BufferedPtySession,
  geometry?: { cols?: number; rows?: number }
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

      if (opts.requireBold) {
        if (opts.surface === 'strippedTranscript') {
          throw new Error(
            'waitForTextInSurface: requireBold is only supported for viewableBuffer and fullBuffer.'
          )
        }
        if (typeof opts.needle !== 'string') {
          throw new Error(
            'waitForTextInSurface: requireBold requires a string needle.'
          )
        }
      }

      if (
        opts.rejectGrayForegroundOnlyWithoutGrayBackground ||
        opts.requireGrayBackgroundBlock
      ) {
        if (opts.surface === 'strippedTranscript') {
          throw new Error(
            'waitForTextInSurface: gray block options are only supported for viewableBuffer and fullBuffer.'
          )
        }
        if (typeof opts.needle !== 'string') {
          throw new Error(
            'waitForTextInSurface: gray block options require a string needle.'
          )
        }
      }

      const timeoutMs = opts.timeoutMs ?? 0
      const retryMs = opts.retryMs ?? TTY_ASSERT_LOCATOR_DEFAULT_RETRY_MS
      const strict = opts.strict ?? true
      const messagePrefix = opts.messagePrefix

      const started = Date.now()
      let lastFail: { snapshot: string; detail: string } | undefined

      for (;;) {
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
          await syncReplay()
          result = attemptOnceOnLiveTerminal(term, {
            needle: opts.needle,
            surface: opts.surface,
            raw,
            strict,
            cols,
            rows,
            startAfterAnchor: opts.startAfterAnchor,
            fallbackRowCount: opts.fallbackRowCount,
            requireBold: opts.requireBold,
            rejectGrayForegroundOnlyWithoutGrayBackground:
              opts.rejectGrayForegroundOnlyWithoutGrayBackground,
            requireGrayBackgroundBlock: opts.requireGrayBackgroundBlock,
          })
        }

        if (result.ok === true) return

        if (result.ok === 'strict') {
          const body = withOptionalMessagePrefix(
            messagePrefix,
            formatFailureMessage(opts.surface, result.message, result.snapshot)
          )
          throw new TtyAssertStrictModeViolationError(
            appendRawTerminalSnapshotForErrorMessage(body, raw)
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
            formatFailureMessage(opts.surface, detail, lastFail.snapshot)
          )
          throw new Error(appendRawTerminalSnapshotForErrorMessage(body, raw))
        }
        await sleep(retryMs)
      }
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
          tailPreview(raw.slice(-800))
        ),
      }
    },
    dispose() {
      if (disposed) return
      disposed = true
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
  const session = await startBufferedPtySession(opts)
  return attachManagedTtySession(session, geometry)
}
