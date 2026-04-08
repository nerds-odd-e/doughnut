import type { IPty } from '@lydell/node-pty'
import { Terminal } from '@xterm/headless'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { attachManagedTtySession } from '../src/managedTtySession'
import { ptyTranscriptToViewportPlaintext } from '../src/ptyTranscriptToVisiblePlaintextViaXterm'

function mockPty(): IPty {
  const noop = () => {
    /* mock PTY — tests drive buf.text only */
  }
  return {
    write: noop,
    kill: noop,
    onData: noop,
  } as unknown as IPty
}

describe('ManagedTtySession', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('assert() does not take raw; finds text via internal buffer (strippedTranscript)', async () => {
    const buf = { text: 'hello \x1b[31mWORLD\x1b[0m' }
    const m = attachManagedTtySession({ pty: mockPty(), buf })
    try {
      await m.assert({
        needle: 'WORLD',
        surface: 'strippedTranscript',
        timeoutMs: 0,
      })
    } finally {
      m.dispose()
    }
  })

  it('incremental sync: second viewableBuffer assert writes only new PTY bytes', async () => {
    const writeSpy = vi.spyOn(Terminal.prototype, 'write')
    const buf = { text: '' }
    const m = attachManagedTtySession(
      { pty: mockPty(), buf },
      { cols: 20, rows: 4 }
    )
    try {
      buf.text = '\x1b[2J\x1b[Haa'
      await m.assert({
        needle: 'aa',
        surface: 'viewableBuffer',
        cols: 20,
        rows: 4,
        timeoutMs: 0,
      })

      const callsAfterFirst = writeSpy.mock.calls.length
      expect(callsAfterFirst).toBeGreaterThan(0)
      const firstPayload = String(writeSpy.mock.calls[0]?.[0] ?? '')
      expect(firstPayload).toContain('aa')

      buf.text += 'bb'
      await m.assert({
        needle: 'bb',
        surface: 'viewableBuffer',
        cols: 20,
        rows: 4,
        timeoutMs: 0,
      })

      expect(writeSpy.mock.calls.length).toBeGreaterThan(callsAfterFirst)
      const lastPayload = String(
        writeSpy.mock.calls[writeSpy.mock.calls.length - 1]?.[0] ?? ''
      )
      expect(lastPayload).toBe('bb')
    } finally {
      m.dispose()
    }
  })

  it('dumpFrames replay previews match viewport plaintext from live terminal', async () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const buf = { text: raw }
    const m = attachManagedTtySession(
      { pty: mockPty(), buf },
      { cols: 20, rows: 4 }
    )
    try {
      await m.assert({
        needle: 'OK',
        surface: 'viewableBuffer',
        cols: 20,
        rows: 4,
        timeoutMs: 0,
      })
      const dumped = await m.dumpFrames()
      const replayed = await ptyTranscriptToViewportPlaintext(raw)
      expect(replayed).toMatch(/^OK/)
      expect(dumped.replayedScreenPlaintextHeadPreview).toContain('OK')
      expect(dumped.replayedScreenPlaintextHeadPreview).toBe(
        replayed.length <= 500 ? replayed : `${replayed.slice(0, 500)}...`
      )
    } finally {
      m.dispose()
    }
  })

  it('rebuilds xterm from full transcript when buffer shrinks (replay desync)', async () => {
    const buf = { text: '' }
    const m = attachManagedTtySession(
      { pty: mockPty(), buf },
      { cols: 20, rows: 4 }
    )
    try {
      buf.text = `${'x'.repeat(60)}\x1b[2J\x1b[Hfirst`
      await m.assert({
        needle: 'first',
        surface: 'viewableBuffer',
        cols: 20,
        rows: 4,
        timeoutMs: 0,
      })

      buf.text = '\x1b[2J\x1b[Hafter'
      await m.assert({
        needle: 'after',
        surface: 'viewableBuffer',
        cols: 20,
        rows: 4,
        timeoutMs: 0,
      })
    } finally {
      m.dispose()
    }
  })

  it('dispose() is idempotent (double dispose does not throw)', () => {
    const m = attachManagedTtySession({ pty: mockPty(), buf: { text: '' } })
    m.dispose()
    expect(() => m.dispose()).not.toThrow()
  })

  it('dispose() does not propagate when pty.kill throws (child already exited)', () => {
    const pty = mockPty()
    vi.spyOn(pty, 'kill').mockImplementation(() => {
      throw new Error('already exited')
    })
    const m = attachManagedTtySession({ pty, buf: { text: '' } })
    expect(() => m.dispose()).not.toThrow()
  })

  it('assert times out when startup-style marker never appears (strippedTranscript)', async () => {
    const buf = { text: 'no marker here' }
    const m = attachManagedTtySession({ pty: mockPty(), buf })
    try {
      await expect(
        m.assert({
          needle: 'doughnut 0.1.0',
          surface: 'strippedTranscript',
          timeoutMs: 120,
          retryMs: 20,
        })
      ).rejects.toThrow(/Timeout after 120ms/)
    } finally {
      m.dispose()
    }
  })

  it('assert aborts after dispose during retry wait', async () => {
    const buf = { text: 'nope' }
    const m = attachManagedTtySession(
      { pty: mockPty(), buf },
      { cols: 20, rows: 4 }
    )
    queueMicrotask(() => {
      m.dispose()
    })
    await expect(
      m.assert({
        needle: 'missing',
        surface: 'strippedTranscript',
        timeoutMs: 60_000,
        retryMs: 5,
      })
    ).rejects.toThrow('ManagedTtySession.assert after dispose')
  })
})
