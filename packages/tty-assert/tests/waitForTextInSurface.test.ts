import { describe, expect, it, vi } from 'vitest'
import {
  TtyAssertStrictModeViolationError,
  waitForTextInSurface,
} from '../src/waitForTextInSurface'

describe('waitForTextInSurface', () => {
  it('finds a string in fullBuffer using row-major flat search (no newlines between rows)', async () => {
    const raw = '\x1b[2J\x1b[Hhello'
    await waitForTextInSurface({
      raw,
      needle: 'hello',
      surface: 'fullBuffer',
      cols: 20,
      rows: 4,
      timeoutMs: 0,
    })
  })

  it('finds text in strippedTranscript ignoring ANSI', async () => {
    await waitForTextInSurface({
      raw: 'before \x1b[31m\x1b[0mTOKEN\x1b[m after',
      needle: 'TOKEN',
      surface: 'strippedTranscript',
      timeoutMs: 0,
    })
  })

  it('matches RegExp needles', async () => {
    await waitForTextInSurface({
      raw: 'foo bar baz',
      needle: /bar baz/,
      surface: 'strippedTranscript',
      timeoutMs: 0,
    })
  })

  it('defaults strict to true and throws when the needle matches twice', async () => {
    await expect(
      waitForTextInSurface({
        raw: 'hi hi',
        needle: 'hi',
        surface: 'strippedTranscript',
        timeoutMs: 0,
      })
    ).rejects.toThrow(TtyAssertStrictModeViolationError)
  })

  it('allows multiple matches when strict is false', async () => {
    await waitForTextInSurface({
      raw: 'hi hi',
      needle: 'hi',
      surface: 'strippedTranscript',
      strict: false,
      timeoutMs: 0,
    })
  })

  it('failure message names the surface and includes a snapshot block', async () => {
    await expect(
      waitForTextInSurface({
        raw: 'only this',
        needle: 'missing',
        surface: 'viewableBuffer',
        cols: 10,
        rows: 2,
        timeoutMs: 0,
      })
    ).rejects.toThrow(/surface "viewableBuffer"[\s\S]*---\n/)
  })

  it('failure message includes ANSI-stripped raw snapshot and optional messagePrefix', async () => {
    const err = await waitForTextInSurface({
      raw: 'pty-bytes',
      needle: 'nope',
      surface: 'strippedTranscript',
      timeoutMs: 0,
      messagePrefix: 'Custom prefix.',
    }).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(Error)
    const msg = (err as Error).message
    expect(msg).toMatch(/^Custom prefix\.\n/)
    expect(msg).toContain('raw bytes:')
    expect(msg).toContain('ANSI-stripped:')
  })

  it('with timeoutMs 0, failure does not say "Timeout after 0ms" (single-shot callers)', async () => {
    const err = await waitForTextInSurface({
      raw: 'x',
      needle: 'nope',
      surface: 'strippedTranscript',
      timeoutMs: 0,
    }).catch((e: unknown) => e)
    expect(err).toBeInstanceOf(Error)
    expect((err as Error).message).not.toContain('Timeout after 0ms')
  })

  it('fullBuffer retains scrolled-off lines that viewableBuffer may omit', async () => {
    const rows = 3
    const cols = 50
    const raw = Array.from({ length: 30 }, (_, i) => `LINE_${i}_MARK\n`).join(
      ''
    )
    await waitForTextInSurface({
      raw,
      needle: 'LINE_0_MARK',
      surface: 'fullBuffer',
      cols,
      rows,
      timeoutMs: 0,
    })
    await expect(
      waitForTextInSurface({
        raw,
        needle: 'LINE_0_MARK',
        surface: 'viewableBuffer',
        cols,
        rows,
        timeoutMs: 0,
      })
    ).rejects.toThrow(/not found/)
  })

  it('requireBold treats xterm bold as any non-zero isBold()', async () => {
    const raw = `\x1b[2J\x1b[H\x1b[1mPut\x1b[22m x\n`
    await waitForTextInSurface({
      raw,
      needle: 'Put',
      surface: 'viewableBuffer',
      cols: 80,
      rows: 24,
      timeoutMs: 0,
      requireBold: true,
    })
  })

  it('rejects requireBold with strippedTranscript at call time', async () => {
    await expect(
      waitForTextInSurface({
        raw: 'x',
        needle: 'x',
        surface: 'strippedTranscript',
        timeoutMs: 0,
        requireBold: true,
      })
    ).rejects.toThrow(/requireBold is only supported/)
  })

  it('requireBold fails when the first match is not all bold', async () => {
    const raw = `\x1b[2J\x1b[HPlain\x1b[1mBold\x1b[22m\n`
    await expect(
      waitForTextInSurface({
        raw,
        needle: 'Plain',
        surface: 'viewableBuffer',
        cols: 80,
        rows: 24,
        timeoutMs: 0,
        requireBold: true,
      })
    ).rejects.toThrow(/not all cells at the first occurrence are bold/)
  })

  it('polls until a getter returns raw that contains the needle', async () => {
    vi.useFakeTimers()
    const state = { text: 'start' }
    const p = waitForTextInSurface({
      raw: () => state.text,
      needle: 'ready',
      surface: 'strippedTranscript',
      timeoutMs: 10_000,
      retryMs: 5,
    })
    await vi.advanceTimersByTimeAsync(0)
    state.text = 'almost ready'
    await vi.advanceTimersByTimeAsync(20)
    state.text = 'now ready'
    await vi.advanceTimersByTimeAsync(20)
    await p
    vi.useRealTimers()
  })
})
