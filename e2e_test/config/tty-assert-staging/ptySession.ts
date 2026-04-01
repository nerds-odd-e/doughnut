import type { IPty } from '@lydell/node-pty'
import { CLI_INTERACTIVE_PTY_COLS, CLI_INTERACTIVE_PTY_ROWS } from './geometry'
import { headPreview } from './errorSnapshotFormatting'
import { stripAnsiCliPty } from './stripAnsi'

export type BufferedPtySession = {
  pty: IPty
  buf: { text: string }
}

export type StartBufferedPtySessionOptions = {
  command: string
  args: string[]
  cwd: string
  env: NodeJS.ProcessEnv
  cols?: number
  rows?: number
}

export async function startBufferedPtySession(
  opts: StartBufferedPtySessionOptions
): Promise<BufferedPtySession> {
  const { spawn } = await import('@lydell/node-pty')
  const p = spawn(opts.command, opts.args, {
    name: 'xterm-256color',
    cols: opts.cols ?? CLI_INTERACTIVE_PTY_COLS,
    rows: opts.rows ?? CLI_INTERACTIVE_PTY_ROWS,
    cwd: opts.cwd,
    env: opts.env,
  })
  const buf = { text: '' }
  p.onData((data: string) => {
    buf.text += data
  })
  return { pty: p, buf }
}

export function disposeBufferedPtySession(
  session: BufferedPtySession | null | undefined
): void {
  if (!session) return
  try {
    session.pty.kill()
  } catch {
    /* already exited */
  }
}

export function waitForVisiblePlaintextSubstring(
  getRawOutput: () => string,
  needle: string,
  timeoutMs: number
): Promise<void> {
  const started = Date.now()
  return new Promise((resolve, reject) => {
    const tick = () => {
      const stripped = stripAnsiCliPty(getRawOutput())
      if (stripped.includes(needle)) {
        resolve()
        return
      }
      if (Date.now() - started >= timeoutMs) {
        const preview = headPreview(stripped)
        reject(
          new Error(
            `Timeout after ${timeoutMs}ms waiting for substring ${JSON.stringify(needle)} in interactive CLI PTY output. Preview (ANSI-stripped):\n${preview}`
          )
        )
        return
      }
      setTimeout(tick, 50)
    }
    tick()
  })
}
