/**
 * Node-only sketch of a terminal assertion API over a buffered PTY session.
 *
 * **Stripped transcript vs replayed screen:** `getByText` + `expect(…).toBeVisible()` search the
 * **ANSI-stripped cumulative PTY transcript** (same model as plugin startup wait and most
 * `outputAssertions` checks). That is not the same as the emulated viewport from CSI replay; use
 * `getReplayedScreenPlaintext()` when you need replayed “screen” plain text (e.g. current guidance).
 *
 * **`dumpFrames()`:** PTY tests do not have Ink-style discrete frames; this returns structured
 * diagnostics (lengths, previews). A real frame array would require something like xterm (parent
 * plan Phase 4).
 */

import {
  headPreview,
  sanitizeVisibleTextForError,
  tailPreview,
} from './errorSnapshotFormatting'
import { ptyTranscriptToVisiblePlaintext } from './ptyTranscriptToVisiblePlaintext'
import {
  disposeBufferedPtySession,
  startBufferedPtySession,
  waitForVisiblePlaintextSubstring,
  type BufferedPtySession,
  type StartBufferedPtySessionOptions,
} from './ptySession'
import { stripAnsiCliPty } from './stripAnsi'

export const DEFAULT_TTY_EXPECT_TIMEOUT_MS = 3000
export const DEFAULT_TTY_EXPECT_RETRY_MS = 50

export type TtySubstringLocator = {
  kind: 'substring'
  value: string
}

export type TtyAssertToBeVisibleOpts = {
  timeoutMs?: number
  retryMs?: number
}

export type TtyAssertDumpFrames = {
  rawByteLength: number
  ansiStrippedLength: number
  replayedScreenPlaintextHeadPreview: string
  replayedScreenPlaintextTailPreview: string
  strippedTranscriptTailPreview: string
  rawTailSanitizedPreview: string
}

export type TtyAssertTerminalHandle = {
  readonly session: BufferedPtySession
  write(data: string): void
  submit(line: string): void
  kill(): void
  getRawBuffer(): string
  /** Cumulative ANSI-stripped transcript (not emulator viewport). */
  getVisiblePlaintext(): string
  /** Last replayed screen as plain text (CSI replay; same geometry as the PTY). */
  getReplayedScreenPlaintext(): string
  getByText(value: string): TtySubstringLocator
  expect(loc: TtySubstringLocator): {
    toBeVisible(opts?: TtyAssertToBeVisibleOpts): Promise<void>
  }
  /** Diagnostic-only; not a real frame list. */
  dumpFrames(): TtyAssertDumpFrames
}

function createHandle(session: BufferedPtySession): TtyAssertTerminalHandle {
  return {
    session,
    write(data: string) {
      session.pty.write(data)
    },
    submit(line: string) {
      session.pty.write(`${line}\r`)
    },
    kill() {
      disposeBufferedPtySession(session)
    },
    getRawBuffer() {
      return session.buf.text
    },
    getVisiblePlaintext() {
      return stripAnsiCliPty(session.buf.text)
    },
    getReplayedScreenPlaintext() {
      return ptyTranscriptToVisiblePlaintext(session.buf.text)
    },
    getByText(value: string): TtySubstringLocator {
      return { kind: 'substring', value }
    },
    expect(loc: TtySubstringLocator) {
      return {
        toBeVisible: (opts?: TtyAssertToBeVisibleOpts) =>
          waitForVisiblePlaintextSubstring(
            () => session.buf.text,
            loc.value,
            opts?.timeoutMs ?? DEFAULT_TTY_EXPECT_TIMEOUT_MS,
            opts?.retryMs ?? DEFAULT_TTY_EXPECT_RETRY_MS
          ),
      }
    },
    dumpFrames(): TtyAssertDumpFrames {
      const raw = session.buf.text
      const stripped = stripAnsiCliPty(raw)
      const replayed = ptyTranscriptToVisiblePlaintext(raw)
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
  }
}

export async function startProgram(
  opts: StartBufferedPtySessionOptions
): Promise<TtyAssertTerminalHandle> {
  const session = await startBufferedPtySession(opts)
  return attachTerminalHandle(session)
}

export function attachTerminalHandle(
  session: BufferedPtySession
): TtyAssertTerminalHandle {
  return createHandle(session)
}
