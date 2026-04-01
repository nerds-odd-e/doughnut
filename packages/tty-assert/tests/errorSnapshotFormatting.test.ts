import { describe, expect, it } from 'vitest'
import {
  TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS,
  TERMINAL_ERROR_PREVIEW_LEN,
  formatRawTerminalSnapshotForError,
  headPreview,
  sanitizeVisibleTextForError,
  tailPreview,
} from '../src/errorSnapshotFormatting'

describe('sanitizeVisibleTextForError', () => {
  it('normalizes CRLF and lone CR to LF', () => {
    expect(sanitizeVisibleTextForError('a\r\nb\rc')).toBe('a\nb\nc')
  })

  it('replaces ESC with a visible token', () => {
    expect(sanitizeVisibleTextForError('a\x1bbc')).toBe('a<ESC>bc')
  })

  it('keeps tab and newline, hex-escapes other control chars', () => {
    expect(sanitizeVisibleTextForError('x\t\n\x01')).toBe('x\t\n<0x01>')
  })

  it('hex-escapes DEL', () => {
    expect(sanitizeVisibleTextForError('\x7f')).toBe('<0x7f>')
  })
})

describe('headPreview and tailPreview', () => {
  it('returns short text unchanged', () => {
    const s = 'x'.repeat(100)
    expect(headPreview(s)).toBe(s)
    expect(tailPreview(s)).toBe(s)
  })

  it('headPreview truncates at TERMINAL_ERROR_PREVIEW_LEN with ellipsis', () => {
    const s = 'y'.repeat(TERMINAL_ERROR_PREVIEW_LEN + 1)
    const h = headPreview(s)
    expect(h).toHaveLength(TERMINAL_ERROR_PREVIEW_LEN + 3)
    expect(h.endsWith('...')).toBe(true)
    expect(h.startsWith('y'.repeat(TERMINAL_ERROR_PREVIEW_LEN))).toBe(true)
  })

  it('tailPreview keeps the last TERMINAL_ERROR_PREVIEW_LEN chars when longer', () => {
    const s = `prefix${'z'.repeat(TERMINAL_ERROR_PREVIEW_LEN)}`
    expect(tailPreview(s)).toBe('z'.repeat(TERMINAL_ERROR_PREVIEW_LEN))
  })

  it('boundary: exactly preview length is not truncated', () => {
    const s = 'q'.repeat(TERMINAL_ERROR_PREVIEW_LEN)
    expect(headPreview(s)).toBe(s)
    expect(tailPreview(s)).toBe(s)
  })
})

describe('formatRawTerminalSnapshotForError', () => {
  it('includes byte counts and visible body', () => {
    const raw = '\x1b[31mhi\x1b[0m'
    const out = formatRawTerminalSnapshotForError(raw)
    expect(out).toContain('raw bytes:')
    expect(out).toContain('hi')
  })

  it('truncates when sanitized visible text exceeds max', () => {
    const inner = 'w'.repeat(TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS + 50)
    const raw = inner
    const out = formatRawTerminalSnapshotForError(raw)
    expect(out).toContain('truncated')
    expect(out).toContain(`${TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS}`)
    expect(out.length).toBeLessThan(inner.length + 500)
  })
})
