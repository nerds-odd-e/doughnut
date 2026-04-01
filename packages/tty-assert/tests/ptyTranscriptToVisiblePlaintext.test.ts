import { describe, expect, it } from 'vitest'
import { ptyTranscriptToVisiblePlaintext } from '../src/ptyTranscriptToVisiblePlaintext'

describe('ptyTranscriptToVisiblePlaintext', () => {
  it('renders two lines from newlines on a small grid', () => {
    const visible = ptyTranscriptToVisiblePlaintext('hello\nworld', 20, 4)
    expect(visible).toBe('hello\nworld')
  })

  it('carriage return moves cursor to column zero before newline', () => {
    const visible = ptyTranscriptToVisiblePlaintext('ab\r\ncd', 10, 3)
    expect(visible).toBe('ab\ncd')
  })

  it('clears screen and writes from home', () => {
    const raw = `noise\x1b[2J\x1b[HOK`
    const visible = ptyTranscriptToVisiblePlaintext(raw, 10, 3)
    expect(visible).toMatch(/^OK/)
    expect(visible).not.toContain('noise')
  })

  it('wraps to the next row when reaching column limit', () => {
    const visible = ptyTranscriptToVisiblePlaintext('abcdefghij', 4, 3)
    expect(visible).toBe('abcd\nefgh\nij')
  })

  it('scrolls when writing past the last row, keeping the last rows visible', () => {
    const visible = ptyTranscriptToVisiblePlaintext('aa\nbb\ncc\ndd', 4, 2)
    expect(visible).toBe('cc\ndd')
  })
})
