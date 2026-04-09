import type { IPty } from '@lydell/node-pty'
import { describe, expect, it } from 'vitest'
import { attachTerminalHandle } from '../src/facade'

function mockPty(): IPty {
  const noop = () => {
    /* mock PTY — facade tests only touch buf.text */
  }
  return {
    write: noop,
    kill: noop,
    onData: noop,
  } as unknown as IPty
}

describe('facade replay (xterm)', () => {
  it('getReplayedScreenPlaintext uses xterm on fixed raw', async () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const handle = attachTerminalHandle({
      pty: mockPty(),
      buf: { text: raw },
    })
    const replayed = await handle.getReplayedScreenPlaintext()
    expect(replayed).toMatch(/^OK/)
    expect(replayed).not.toContain('noise')
  })

  it('dumpDiagnostics replay previews use the same xterm replay as getReplayedScreenPlaintext', async () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const handle = attachTerminalHandle({
      pty: mockPty(),
      buf: { text: raw },
    })
    const [dumped, replayed] = await Promise.all([
      handle.dumpDiagnostics(),
      handle.getReplayedScreenPlaintext(),
    ])
    expect(replayed).toMatch(/^OK/)
    expect(dumped.replayedScreenPlaintextHeadPreview).toContain('OK')
    expect(dumped.replayedScreenPlaintextHeadPreview).toBe(
      replayed.length <= 500 ? replayed : `${replayed.slice(0, 500)}...`
    )
  })
})
