import { describe, expect, it } from 'vitest'
import { ptyTranscriptToVisiblePlaintext } from '../src/ptyTranscriptToVisiblePlaintext'
import { ptyTranscriptToVisiblePlaintextViaXterm } from '../src/ptyTranscriptToVisiblePlaintextViaXterm'

/**
 * Parity vs hand-rolled replay (`ptyTranscriptToVisiblePlaintext`) for the same
 * fixtures as `ptyTranscriptToVisiblePlaintext.test.ts`. Phase 4.2 gate: xterm
 * matches legacy for Current-guidance–relevant replay, or mismatches are
 * isolated with explicit expected xterm output.
 */
describe('ptyTranscript replay parity (legacy vs xterm)', () => {
  it('renders two lines when using CRLF (PTY-shaped)', async () => {
    const raw = 'hello\r\nworld'
    const cols = 20
    const rows = 4
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(xterm).toBe(legacy)
    expect(legacy).toBe('hello\nworld')
  })

  // Bare LF: legacy advances row and resets column; xterm.js applies VT line feed only (column
  // unchanged). Real PTY output from Node/Ink uses CRLF for newlines, so guidance replay should
  // match on CRLF-shaped transcripts; this fixture documents the intentional delta on LF-only.
  it('bare LF: xterm keeps column (VT LF); legacy resets column', async () => {
    const raw = 'hello\nworld'
    const cols = 20
    const rows = 4
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(legacy).toBe('hello\nworld')
    expect(xterm).toBe('hello\n     world')
  })

  it('SGR-colored CRLF lines match (Ink-style escapes, plain viewport text)', async () => {
    const raw = '\x1b[1mhello\x1b[0m\r\n\x1b[90mworld\x1b[0m'
    const cols = 20
    const rows = 4
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(xterm).toBe(legacy)
    expect(legacy).toBe('hello\nworld')
  })

  it('carriage return moves cursor to column zero before newline', async () => {
    const raw = 'ab\r\ncd'
    const cols = 10
    const rows = 3
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(xterm).toBe(legacy)
  })

  it('clears screen and writes from home', async () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const cols = 10
    const rows = 3
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(xterm).toBe(legacy)
  })

  it('wraps to the next row when reaching column limit', async () => {
    const raw = 'abcdefghij'
    const cols = 4
    const rows = 3
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(xterm).toBe(legacy)
  })

  // Legacy hand-rolled scroll fills the bottom row with spaces when scrolling, so the last
  // full line stays left-aligned. xterm.js scrollback leaves a different column state after the
  // same byte sequence (minimal synthetic fixture). Prefer xterm as the reference for Phase 4.3
  // wiring; guidance extraction uses geometry + real PTY bytes, not this exact edge pattern alone.
  it('scroll past last row: legacy vs xterm differ on this synthetic fixture', async () => {
    const raw = 'aa\nbb\ncc\ndd'
    const cols = 4
    const rows = 2
    const legacy = ptyTranscriptToVisiblePlaintext(raw, cols, rows)
    const xterm = await ptyTranscriptToVisiblePlaintextViaXterm(raw, cols, rows)
    expect(legacy).toBe('cc\ndd')
    expect(xterm).toBe('c\n dd')
  })
})
