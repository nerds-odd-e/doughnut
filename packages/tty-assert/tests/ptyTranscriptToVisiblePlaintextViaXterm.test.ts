import { describe, expect, it } from 'vitest'
import {
  ptyTranscriptToViewportPlaintext,
  ptyTranscriptToVisiblePlaintextViaXterm,
} from '../src/ptyTranscriptToVisiblePlaintextViaXterm'

describe('ptyTranscriptToVisiblePlaintextViaXterm (viewport replay)', () => {
  it('replays a minimal transcript to stable non-empty plain text', async () => {
    const raw = 'hello\nworld'
    const a = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 20, 4)
    const b = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 20, 4)
    expect(a).toBe(b)
    expect(a.length).toBeGreaterThan(0)
    expect(a).toContain('hello')
    expect(a).toContain('world')
  })

  it('ptyTranscriptToViewportPlaintext is the same async function', () => {
    expect(ptyTranscriptToViewportPlaintext).toBe(
      ptyTranscriptToVisiblePlaintextViaXterm
    )
  })

  it('renders two lines when using CRLF (PTY-shaped)', async () => {
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(
      'hello\r\nworld',
      20,
      4
    )
    expect(visible).toBe('hello\nworld')
  })

  // Bare LF: xterm.js applies VT line feed only (column unchanged). Real PTY output from
  // Node/Ink uses CRLF for newlines; LF-only transcripts differ from CRLF-shaped ones.
  it('bare LF: xterm keeps column after VT LF', async () => {
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(
      'hello\nworld',
      20,
      4
    )
    expect(visible).toBe('hello\n     world')
  })

  it('SGR-colored CRLF lines strip to plain viewport text', async () => {
    const raw = '\x1b[1mhello\x1b[0m\r\n\x1b[90mworld\x1b[0m'
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 20, 4)
    expect(visible).toBe('hello\nworld')
  })

  it('carriage return moves cursor to column zero before newline', async () => {
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(
      'ab\r\ncd',
      10,
      3
    )
    expect(visible).toBe('ab\ncd')
  })

  it('clears screen and shows text written after ED', async () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(raw, 10, 3)
    expect(visible).toMatch(/^OK/)
    expect(visible).not.toContain('noise')
  })

  it('wraps to the next row when reaching column limit', async () => {
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(
      'abcdefghij',
      4,
      3
    )
    expect(visible).toBe('abcd\nefgh\nij')
  })

  it('scroll past last row: synthetic multi-line fixture', async () => {
    const visible = await ptyTranscriptToVisiblePlaintextViaXterm(
      'aa\nbb\ncc\ndd',
      4,
      2
    )
    expect(visible).toBe('c\n dd')
  })
})
