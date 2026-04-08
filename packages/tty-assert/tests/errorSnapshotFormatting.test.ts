import { describe, expect, it } from 'vitest'
import {
  TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS,
  TERMINAL_ERROR_MAX_VISIBLE_SNAPSHOT_CHARS,
  TERMINAL_ERROR_PREVIEW_LEN,
  annotateSubstringRangeInText,
  formatRawTerminalSnapshotForError,
  formatSearchSurfaceFailure,
  formatSnapshotWithRowNumbers,
  headPreview,
  sanitizeVisibleTextForError,
  tailPreview,
  truncateLocatorFailureSnapshot,
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

describe('truncateLocatorFailureSnapshot', () => {
  it('leaves short text unchanged', () => {
    expect(truncateLocatorFailureSnapshot('hi')).toBe('hi')
  })

  it('truncates past TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS with ellipsis line', () => {
    const s = 'z'.repeat(TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS + 1)
    const out = truncateLocatorFailureSnapshot(s)
    expect(out.endsWith('\n… (truncated)')).toBe(true)
    expect(out.length).toBeLessThanOrEqual(
      TERMINAL_ERROR_LOCATOR_SNAPSHOT_MAX_CHARS + '\n… (truncated)'.length
    )
  })
})

describe('formatSnapshotWithRowNumbers', () => {
  it('numbers each line with fixed width', () => {
    expect(formatSnapshotWithRowNumbers('a\nbb')).toBe('  1 | a\n  2 | bb')
  })

  it('supports firstLine offset', () => {
    expect(formatSnapshotWithRowNumbers('x', { firstLine: 10 })).toBe(' 10 | x')
  })

  it('widens the gutter for line 100+', () => {
    const lines = Array.from({ length: 100 }, () => '.').join('\n')
    const out = formatSnapshotWithRowNumbers(lines)
    expect(out).toContain(' 99 |')
    expect(out).toContain('100 |')
  })

  it('optional column ruler prepends a tens row', () => {
    const out = formatSnapshotWithRowNumbers('ab', {
      columnRuler: true,
      cols: 12,
    })
    const lines = out.split('\n')
    expect(lines[0]).toMatch(/^\s+\| 012345678901$/)
    expect(lines[1]).toBe('  1 | ab')
  })
})

describe('annotateSubstringRangeInText', () => {
  it('wraps the half-open range [start, end)', () => {
    expect(annotateSubstringRangeInText('abcdef', 2, 4)).toBe('ab«cd»ef')
  })

  it('allows empty range', () => {
    expect(annotateSubstringRangeInText('ab', 1, 1)).toBe('a«»b')
  })

  it('clamps out-of-range indices', () => {
    expect(annotateSubstringRangeInText('x', -5, 99)).toBe('«x»')
  })

  it('swaps when start > end after clamping', () => {
    expect(annotateSubstringRangeInText('abcde', 4, 1)).toBe('a«bcd»e')
  })

  it('treats non-finite as 0', () => {
    expect(annotateSubstringRangeInText('hi', Number.NaN, 1)).toBe('«h»i')
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
})
