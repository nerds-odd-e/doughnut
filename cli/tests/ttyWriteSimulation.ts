/**
 * Helpers for interpreting raw `process.stdout.write` chunks from the interactive TTY adapter:
 * simulate cursor movement / erase, and detect repaint bugs (e.g. stale CUU before the input box).
 */
import { CLEAR_SCREEN, stripAnsiCsiAndCr } from '../src/renderer.js'

/** Top border line of the bordered input box (plain text, after stripping ANSI). */
export const INPUT_BOX_TOP_OUTLINE_PATTERN = /^┌─*┐$/

/**
 * True when a live-region clear is immediately followed by an extra cursor-up before the first
 * full-line erase that draws the input box top (`┌`). That sequence shifts the box one row up.
 */
export function liveRegionRepaintHasStaleCursorUpBeforeBoxTop(
  rawWritesJoined: string
): boolean {
  const marker = '\r\x1b[2K┌'
  const idx = rawWritesJoined.indexOf(marker)
  if (idx < 0) return false
  const prefix = rawWritesJoined.slice(0, idx)
  // biome-ignore lint/suspicious/noControlCharactersInRegex: CSI CUU
  return /\x1b\[\d+A\x1b\[\d+A$/.test(prefix)
}

export function countInputBoxTopOutlinesBeforeFirstBoxContent(
  rawWritesJoined: string
): number {
  const normalized = stripAnsiCsiAndCr(rawWritesJoined)
  const lines = normalized.split('\n')
  let searchStart = 0
  for (let i = 0; i < lines.length; i++) {
    if (/doughnut \d+\.\d+\.\d+/.test(lines[i] ?? '')) {
      searchStart = i + 1
      break
    }
  }
  let count = 0
  for (let i = searchStart; i < lines.length; i++) {
    const line = (lines[i] ?? '').trim()
    if (line.includes('│')) {
      break
    }
    if (INPUT_BOX_TOP_OUTLINE_PATTERN.test(line)) {
      count++
    }
  }
  return count
}

/** Apply CUU/CUD, EL, CUP, and printable chars to build a naive terminal frame (tests only). */
export function simulatedScreenFromTtyWrites(output: string): string {
  const lines: string[] = []
  let row = 0
  let col = 0
  let i = 0
  const ESC = '\x1b'
  const cursorUpRe = new RegExp(`^${ESC}\\[(\\d+)A`)
  const cursorDownRe = new RegExp(`^${ESC}\\[(\\d+)B`)
  const eraseLineRe = new RegExp(`^${ESC}\\[2K`)
  const cursorColRe = new RegExp(`^${ESC}\\[(\\d+)G`)
  const csiRe = new RegExp(`^${ESC}\\[[\\d;?]*[A-Za-z]`)
  while (i < output.length) {
    if (output.startsWith('\x1b[', i)) {
      const rest = output.slice(i)
      const cursorUpMatch = rest.match(cursorUpRe)
      const cursorDownMatch = rest.match(cursorDownRe)
      const eraseLineMatch = rest.match(eraseLineRe)
      const cursorColMatch = rest.match(cursorColRe)
      if (cursorUpMatch) {
        row = Math.max(0, row - Number(cursorUpMatch[1]))
        i += cursorUpMatch[0].length
        continue
      }
      if (cursorDownMatch) {
        row += Number(cursorDownMatch[1])
        i += cursorDownMatch[0].length
        continue
      }
      if (eraseLineMatch) {
        while (lines.length <= row) lines.push('')
        lines[row] = ''
        col = 0
        i += eraseLineMatch[0].length
        continue
      }
      if (cursorColMatch) {
        col = Number(cursorColMatch[1]) - 1
        i += cursorColMatch[0].length
        continue
      }
      const csiMatch = rest.match(csiRe)
      if (csiMatch) {
        i += csiMatch[0].length
        continue
      }
    }
    if (output[i] === '\r') {
      col = 0
      i++
      continue
    }
    if (output[i] === '\n') {
      row++
      col = 0
      i++
      continue
    }
    while (lines.length <= row) lines.push('')
    const line = lines[row] ?? ''
    lines[row] = line.slice(0, col) + output[i] + line.slice(col + 1)
    col++
    i++
  }
  return lines.join('\n')
}

/**
 * Replays TTY writes and returns final cursor position plus the sparse line buffer (tests only).
 * Handles CUU/CUD, EL 2K, CUP column (G), printable text, newlines, and full-screen clear (`CLEAR_SCREEN`).
 */
export function cursorPositionAfterTtyWrites(output: string): {
  row: number
  col: number
  lines: string[]
} {
  const lines: string[] = []
  let row = 0
  let col = 0
  let i = 0
  const ESC = '\x1b'
  const cursorUpRe = new RegExp(`^${ESC}\\[(\\d+)A`)
  const cursorDownRe = new RegExp(`^${ESC}\\[(\\d+)B`)
  const eraseLineRe = new RegExp(`^${ESC}\\[2K`)
  const cursorColRe = new RegExp(`^${ESC}\\[(\\d+)G`)
  const csiRe = new RegExp(`^${ESC}\\[[\\d;?]*[A-Za-z]`)
  while (i < output.length) {
    if (output.startsWith(CLEAR_SCREEN, i)) {
      lines.length = 0
      row = 0
      col = 0
      i += CLEAR_SCREEN.length
      continue
    }
    if (output[i] === ESC && output[i + 1] === ']') {
      const bel = output.indexOf('\x07', i)
      if (bel >= 0) {
        i = bel + 1
        continue
      }
    }
    if (output.startsWith('\x1b[', i)) {
      const rest = output.slice(i)
      const cursorUpMatch = rest.match(cursorUpRe)
      const cursorDownMatch = rest.match(cursorDownRe)
      const eraseLineMatch = rest.match(eraseLineRe)
      const cursorColMatch = rest.match(cursorColRe)
      if (cursorUpMatch) {
        row = Math.max(0, row - Number(cursorUpMatch[1]))
        i += cursorUpMatch[0].length
        continue
      }
      if (cursorDownMatch) {
        row += Number(cursorDownMatch[1])
        i += cursorDownMatch[0].length
        continue
      }
      if (eraseLineMatch) {
        while (lines.length <= row) lines.push('')
        lines[row] = ''
        col = 0
        i += eraseLineMatch[0].length
        continue
      }
      if (cursorColMatch) {
        col = Number(cursorColMatch[1]) - 1
        i += cursorColMatch[0].length
        continue
      }
      const csiMatch = rest.match(csiRe)
      if (csiMatch) {
        i += csiMatch[0].length
        continue
      }
    }
    if (output[i] === '\r') {
      col = 0
      i++
      continue
    }
    if (output[i] === '\n') {
      row++
      col = 0
      i++
      continue
    }
    while (lines.length <= row) lines.push('')
    const line = lines[row] ?? ''
    lines[row] = line.slice(0, col) + output[i] + line.slice(col + 1)
    col++
    i++
  }
  return { row, col, lines }
}

/** Last row index whose plain text contains `substring` (after `plain` transform). Returns -1 if none. */
export function lastRowIndexContainingPlain(
  lines: readonly string[],
  substring: string,
  plain: (line: string) => string = stripAnsiCsiAndCr
): number {
  let last = -1
  for (let i = 0; i < lines.length; i++) {
    if (plain(lines[i] ?? '').includes(substring)) last = i
  }
  return last
}

/** Counts runs of truly empty simulated lines (`""`), not space-only padding (e.g. grey past-input rows). */
export function maxConsecutiveBlankLines(lines: string[]): number {
  let max = 0
  let curr = 0
  for (const l of lines) {
    if (l === '') {
      curr += 1
      max = Math.max(max, curr)
    } else {
      curr = 0
    }
  }
  return max
}
