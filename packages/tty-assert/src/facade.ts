/**
 * Node-only sketch of a terminal assertion API over a buffered PTY session.
 *
 * **Stripped transcript vs replayed screen:** `getByText` + `expect(…).toBeVisible()` search the
 * **ANSI-stripped cumulative PTY transcript** (same model as plugin startup wait and most
 * `outputAssertions` checks). That is not the same as the xterm-emulated viewport; use
 * `await getReplayedScreenPlaintext()` when you need replayed “screen” plain text (e.g. current guidance).
 *
 * **`dumpDiagnostics()`:** returns structured diagnostics (lengths, previews), not a frame timeline.
 * Replay-derived fields use the same **xterm.js** pipeline as `getReplayedScreenPlaintext()`.
 */

import {
  buildTtyAssertDumpDiagnostics,
  type TtyAssertDumpDiagnostics,
} from './ttyAssertDumpDiagnostics'
import { ptyTranscriptToViewportPlaintext } from './ptyTranscriptToVisiblePlaintextViaXterm'
import {
  disposeBufferedPtySession,
  startBufferedPtySession,
  waitForVisiblePlaintextSubstring,
  type BufferedPtySession,
  type StartBufferedPtySessionOptions,
} from './ptySession'
import { stripAnsiCliPty } from './stripAnsi'

const DEFAULT_TO_BE_VISIBLE_TIMEOUT_MS = 3000
export const DEFAULT_TTY_EXPECT_RETRY_MS = 50

export type TtySubstringLocator = {
  kind: 'substring'
  value: string
}

export type TtyAssertToBeVisibleOpts = {
  timeoutMs?: number
  retryMs?: number
}

export type { TtyAssertDumpDiagnostics }

export type TtyAssertTerminalHandle = {
  readonly session: BufferedPtySession
  write(data: string): void
  submit(line: string): void
  kill(): void
  getRawBuffer(): string
  /** Cumulative ANSI-stripped transcript (not emulator viewport). */
  getVisiblePlaintext(): string
  /** Last replayed screen as plain text (xterm replay; same geometry as the PTY). */
  getReplayedScreenPlaintext(): Promise<string>
  getByText(value: string): TtySubstringLocator
  expect(loc: TtySubstringLocator): {
    toBeVisible(opts?: TtyAssertToBeVisibleOpts): Promise<void>
  }
  /** Diagnostic snapshot (previews, lengths); not a time-ordered frame list. */
  dumpDiagnostics(): Promise<TtyAssertDumpDiagnostics>
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
      return ptyTranscriptToViewportPlaintext(session.buf.text)
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
            opts?.timeoutMs ?? DEFAULT_TO_BE_VISIBLE_TIMEOUT_MS,
            opts?.retryMs ?? DEFAULT_TTY_EXPECT_RETRY_MS
          ),
      }
    },
    async dumpDiagnostics(): Promise<TtyAssertDumpDiagnostics> {
      const raw = session.buf.text
      const replayed = await ptyTranscriptToViewportPlaintext(raw)
      return buildTtyAssertDumpDiagnostics({ raw, replayedPlain: replayed })
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
