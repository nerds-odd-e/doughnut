/**
 * Grapheme-aware terminal column width, ANSI-preserving wrap/truncate, and padding helpers.
 * Used by `renderer.ts` for Ink string props and non-Ink paths (e.g. `processInput` logs).
 */
import { RESET } from './ansi.js'

// biome-ignore lint/suspicious/noControlCharactersInRegex: stripping ANSI escapes
const ANSI_PATTERN = /\x1b\[[0-9;]*m/g
// biome-ignore lint/suspicious/noControlCharactersInRegex: SGR at start of slice
const SGR_AT_START = /^\x1b\[[0-9;]*m/

const graphemeSegmenter = new Intl.Segmenter('en', { granularity: 'grapheme' })

/** Strip ANSI escape sequences; returns plain text. */
export function stripAnsi(str: string): string {
  return str.replace(ANSI_PATTERN, '')
}

/** Strip SGR, other CSI sequences, and CR — for inspecting raw TTY captures in tests. */
export function stripAnsiCsiAndCr(str: string): string {
  const esc = '\x1b'
  return str
    .replace(new RegExp(`${esc}\\[[0-9;]*m`, 'g'), '')
    .replace(new RegExp(`${esc}\\[[0-9;]*[A-Za-z]`, 'g'), '')
    .replace(/\r/g, '')
}

function isRegionalIndicator(cp: number): boolean {
  return cp >= 0x1f1e6 && cp <= 0x1f1ff
}

/**
 * Column width of one Unicode code point (plain text, not a full grapheme).
 * Used only inside {@link terminalColumnsOfPlainGrapheme}.
 */
function codePointTerminalColumnWidth(cp: number): number {
  if (cp === 0x09) return 1
  if (cp < 0x20 || cp === 0x7f) return 0
  if (
    (cp >= 0x0300 && cp <= 0x036f) ||
    (cp >= 0x1ab0 && cp <= 0x1aff) ||
    (cp >= 0x1dc0 && cp <= 0x1dff) ||
    (cp >= 0x20d0 && cp <= 0x20ff) ||
    (cp >= 0xfe00 && cp <= 0xfe0e) ||
    (cp >= 0xfe20 && cp <= 0xfe2f) ||
    (cp >= 0x1f3fb && cp <= 0x1f3ff)
  ) {
    return 0
  }
  if (cp === 0xfe0f) return 0
  if (
    (cp >= 0x2e80 && cp <= 0x303e) ||
    (cp >= 0x3040 && cp <= 0x33ff) ||
    (cp >= 0x3400 && cp <= 0x4dbf) ||
    (cp >= 0x4e00 && cp <= 0xa4cf) ||
    (cp >= 0xa960 && cp <= 0xa97f) ||
    (cp >= 0xac00 && cp <= 0xd7ff) ||
    (cp >= 0xf900 && cp <= 0xfaff) ||
    (cp >= 0xfe10 && cp <= 0xfe6f) ||
    (cp >= 0xff00 && cp <= 0xff60) ||
    (cp >= 0xffe0 && cp <= 0xffe6) ||
    (cp >= 0x1b000 && cp <= 0x1b12f) ||
    (cp >= 0x20000 && cp <= 0x2fffd) ||
    (cp >= 0x30000 && cp <= 0x3fffd)
  ) {
    return 2
  }
  if (
    (cp >= 0x2600 && cp <= 0x26ff) ||
    (cp >= 0x2700 && cp <= 0x27bf) ||
    (cp >= 0x1f000 && cp <= 0x1faff)
  ) {
    return 2
  }
  return 1
}

/**
 * Terminal column width of one plain-text grapheme cluster (no ANSI).
 * Single source of truth for “how wide is this on a typical terminal?” — CJK, emoji,
 * flags (regional indicators), and ZWJ sequences.
 */
export function terminalColumnsOfPlainGrapheme(g: string): number {
  if (g.includes('\u200d')) return 2
  const cps: number[] = []
  for (const ch of g) cps.push(ch.codePointAt(0)!)
  if (cps.length === 2 && cps.every(isRegionalIndicator)) return 2
  if (g.includes('\ufe0f')) return 2
  let sum = 0
  for (const cp of cps) sum += codePointTerminalColumnWidth(cp)
  return sum
}

/** Terminal column count after stripping ANSI; uses grapheme clusters + {@link terminalColumnsOfPlainGrapheme}. */
export function visibleLength(str: string): number {
  let total = 0
  for (const { segment } of graphemeSegmenter.segment(stripAnsi(str))) {
    total += terminalColumnsOfPlainGrapheme(segment)
  }
  return total
}

function* terminalVisualTokens(str: string): Generator<string> {
  let pos = 0
  while (pos < str.length) {
    const slice = str.slice(pos)
    const ansi = slice.match(SGR_AT_START)
    if (ansi) {
      yield ansi[0]
      pos += ansi[0].length
      continue
    }
    const rest = str.slice(pos)
    if (rest.length === 0) break
    for (const { segment } of graphemeSegmenter.segment(rest)) {
      yield segment
      pos += segment.length
      break
    }
  }
}

function terminalTokenVisibleWidth(token: string): number {
  if (token.startsWith('\x1b')) return 0
  return terminalColumnsOfPlainGrapheme(token)
}

/** Wrap one paragraph to `width` visible columns; preserves ANSI sequences. */
export function wrapTextToVisibleWidthLines(
  text: string,
  width: number
): string[] {
  if (width <= 0) return text.length ? [text] : []
  if (!text) return []
  if (visibleLength(text) <= width) return [text]
  const tokens = [...terminalVisualTokens(text)]
  const lines: string[] = []
  let i = 0
  while (i < tokens.length) {
    let line = ''
    let vis = 0
    let breakAfter = -1
    let breakLine = ''
    let j = i
    while (j < tokens.length) {
      const t = tokens[j]!
      if (t.startsWith('\x1b')) {
        line += t
        j++
        continue
      }
      const tw = terminalTokenVisibleWidth(t)
      if (vis + tw > width) break
      if (t === ' ' || t === '\t') {
        breakAfter = j
        breakLine = line + t
      }
      line += t
      vis += tw
      j++
    }
    if (j === i) {
      const t = tokens[i]!
      if (t.startsWith('\x1b')) {
        line = t
        i++
        while (i < tokens.length && tokens[i]!.startsWith('\x1b')) {
          line += tokens[i]!
          i++
        }
        lines.push(line)
        continue
      }
      lines.push(t)
      i++
      continue
    }
    if (j < tokens.length && breakAfter >= i) {
      lines.push(breakLine.trimEnd())
      i = breakAfter + 1
    } else {
      lines.push(line.trimEnd())
      i = j
    }
    while (i < tokens.length && tokens[i] === ' ') i++
  }
  return lines
}

/** Truncate str to at most width visible chars; append "..." when truncating. ANSI-aware. */
export function truncateToWidth(str: string, width: number): string {
  if (visibleLength(str) <= width) return str
  const maxVisible = width - 3
  let visibleCount = 0
  let result = ''
  for (const token of terminalVisualTokens(str)) {
    if (token.startsWith('\x1b')) {
      result += token
    } else {
      const tw = terminalTokenVisibleWidth(token)
      if (visibleCount + tw > maxVisible) {
        return `${result}...${RESET}`
      }
      result += token
      visibleCount += tw
    }
  }
  return result
}

export function padEndVisible(str: string, targetLen: number): string {
  const pad = targetLen - visibleLength(str)
  return pad > 0 ? str + ' '.repeat(pad) : str
}

export function stripTrailingSgrReset(s: string): string {
  let out = s
  // biome-ignore lint/suspicious/noControlCharactersInRegex: strip trailing SGR closes (chalk or full reset)
  const trailing = /\x1b\[(?:0|39|49|27|23|22)m$/
  while (trailing.test(out)) {
    out = out.replace(trailing, '')
  }
  return out
}
