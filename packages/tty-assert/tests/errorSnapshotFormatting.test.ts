import { describe, expect, it } from 'vitest'
import {
  TERMINAL_ERROR_FINAL_VIEWPORT_HEADING,
  formatFinalViewportPlaintextForError,
  formatRawTerminalSnapshotForError,
  formatSearchSurfaceFailure,
  headPreview,
  sanitizeVisibleTextForError,
  tailPreview,
} from '../src/diagnostics/errorSnapshotFormatting'

const PREVIEW_LEN = 500
const MAX_VISIBLE_SNAPSHOT_CHARS = 12_000
const LOCATOR_SNAPSHOT_MAX_CHARS = 8000

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

  it('headPreview truncates at preview length with ellipsis', () => {
    const s = 'y'.repeat(PREVIEW_LEN + 1)
    const h = headPreview(s)
    expect(h).toHaveLength(PREVIEW_LEN + 3)
    expect(h.endsWith('...')).toBe(true)
    expect(h.startsWith('y'.repeat(PREVIEW_LEN))).toBe(true)
  })

  it('tailPreview keeps the last preview-length chars when longer', () => {
    const s = `prefix${'z'.repeat(PREVIEW_LEN)}`
    expect(tailPreview(s)).toBe('z'.repeat(PREVIEW_LEN))
  })

  it('boundary: exactly preview length is not truncated', () => {
    const s = 'q'.repeat(PREVIEW_LEN)
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
    const inner = 'w'.repeat(MAX_VISIBLE_SNAPSHOT_CHARS + 50)
    const raw = inner
    const out = formatRawTerminalSnapshotForError(raw)
    expect(out).toContain('truncated')
    expect(out).toContain(`${MAX_VISIBLE_SNAPSHOT_CHARS}`)
    expect(out.length).toBeLessThan(inner.length + 500)
  })
})

describe('formatFinalViewportPlaintextForError', () => {
  it('includes heading, numbered rows, and closing ---', () => {
    const out = formatFinalViewportPlaintextForError('lineA\nlineB')
    expect(out).toContain(TERMINAL_ERROR_FINAL_VIEWPORT_HEADING)
    expect(out).toContain('  1 | lineA')
    expect(out).toContain('  2 | lineB')
    expect(out.endsWith('\n---')).toBe(true)
  })
})

describe('formatSearchSurfaceFailure', () => {
  it('includes row numbers inside the --- block for multi-line snapshot', () => {
    const body = formatSearchSurfaceFailure(
      'viewableBuffer',
      'Detail here.',
      'rowA\nrowB'
    )
    expect(body).toContain('Search surface: "viewableBuffer"')
    expect(body).toContain('  1 | rowA')
    expect(body).toContain('  2 | rowB')
    expect(body).toMatch(/---\n[\s\S]*\n---/)
  })

  it('single-line stripped snapshot is still line 1', () => {
    const body = formatSearchSurfaceFailure(
      'strippedTranscript',
      'x',
      'one long line'
    )
    expect(body).toContain('  1 | one long line')
  })

  it('truncates numbered snapshot past locator max chars', () => {
    const padding = 'z'.repeat(LOCATOR_SNAPSHOT_MAX_CHARS)
    const body = formatSearchSurfaceFailure('strippedTranscript', 'd', padding)
    expect(body).toContain('… (truncated)')
    expect(body.length).toBeLessThan(padding.length + 400)
  })
})
