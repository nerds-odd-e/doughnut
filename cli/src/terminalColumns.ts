import stringWidth from 'string-width'

export function resolvedTerminalWidth(): number {
  const c = process.stdout.columns
  return typeof c === 'number' && c >= 40 ? c : 120
}

/** Matches MainInteractivePrompt / Ink guidance when `stdout.columns` is unset. */
export function inkTerminalColumns(stdoutColumns: number | undefined): number {
  return typeof stdoutColumns === 'number' && stdoutColumns > 0
    ? stdoutColumns
    : 80
}

const ELLIPSIS = '…'

/** Truncate to terminal display width (grapheme clusters + string-width). */
export function truncateToTerminalColumns(
  text: string,
  maxCols: number,
  ellipsis: string = ELLIPSIS
): string {
  if (maxCols < 1) return ''
  if (stringWidth(text) <= maxCols) return text
  const ew = stringWidth(ellipsis)
  if (maxCols <= ew) {
    let acc = ''
    const seg = new Intl.Segmenter('en', { granularity: 'grapheme' })
    for (const { segment } of seg.segment(ellipsis)) {
      const next = acc + segment
      if (stringWidth(next) <= maxCols) acc = next
      else break
    }
    return acc
  }
  const prefixBudget = maxCols - ew
  let acc = ''
  const seg = new Intl.Segmenter('en', { granularity: 'grapheme' })
  for (const { segment } of seg.segment(text)) {
    const next = acc + segment
    if (stringWidth(next) <= prefixBudget) acc = next
    else break
  }
  return (acc === '' ? '' : acc) + ellipsis
}

function breakLongGraphemeRun(text: string, maxWidth: number): string[] {
  if (maxWidth < 1) return [text]
  const lines: string[] = []
  let line = ''
  const seg = new Intl.Segmenter('en', { granularity: 'grapheme' })
  for (const { segment } of seg.segment(text)) {
    const next = line + segment
    if (stringWidth(next) <= maxWidth) {
      line = next
      continue
    }
    if (line !== '') lines.push(line)
    line = segment
  }
  if (line !== '') lines.push(line)
  return lines.length > 0 ? lines : ['']
}

/** Newline-separated rows; lines wider than `maxDisplayCols` split by grapheme (paths, JSON). */
export function splitTextToTerminalRows(
  text: string,
  maxDisplayCols: number
): string[] {
  if (maxDisplayCols < 1) return ['']
  const normalized = text.replace(/\r\n/g, '\n')
  const out: string[] = []
  for (const line of normalized.split('\n')) {
    if (line === '') {
      out.push('')
      continue
    }
    if (stringWidth(line) <= maxDisplayCols) {
      out.push(line)
      continue
    }
    out.push(...breakLongGraphemeRun(line, maxDisplayCols))
  }
  return out
}

function wrapParagraphToWidths(
  text: string,
  firstLineWidth: number,
  otherLineWidth: number
): string[] {
  const words = text.split(/\s+/).filter((w) => w !== '')
  if (words.length === 0) return ['']

  const out: string[] = []
  let line = ''
  let budget = firstLineWidth

  const emitLine = () => {
    if (line !== '') {
      out.push(line)
      line = ''
    }
    budget = otherLineWidth
  }

  for (const word of words) {
    const trial = line === '' ? word : `${line} ${word}`
    if (stringWidth(trial) <= budget) {
      line = trial
      continue
    }
    emitLine()
    if (stringWidth(word) <= budget) {
      line = word
      continue
    }
    for (const chunk of breakLongGraphemeRun(word, budget)) {
      line = chunk
      emitLine()
    }
  }
  if (line !== '') out.push(line)
  return out.length > 0 ? out : ['']
}

/** Plain-text stem / labels; widths are terminal columns (not UTF-16 length). */
export function wrapPlainTextLinesForTerminal(
  text: string,
  width: number
): string[] {
  const t = text.trim()
  if (t === '') return []
  return wrapParagraphToWidths(t, width, width)
}

export type NumberedTerminalListLine = {
  readonly itemIndex: number
  readonly text: string
}

/** One terminal row per item; long labels truncate with ellipsis (token picker, not MCQ). */
export function numberedTerminalListLines(
  items: readonly string[],
  width: number
): NumberedTerminalListLine[] {
  const out: NumberedTerminalListLine[] = []
  for (let i = 0; i < items.length; i++) {
    const prefix = `${i + 1}. `
    const pw = stringWidth(prefix)
    const bodyBudget = Math.max(1, width - pw)
    const body = truncateToTerminalColumns(items[i]!, bodyBudget)
    out.push({ itemIndex: i, text: prefix + body })
  }
  return out
}

/** Numbered list for Current guidance; widths are terminal columns (not UTF-16 length). */
export function formatNumberedListForTerminal(
  items: readonly string[],
  width: number
): string {
  return numberedTerminalListLines(items, width)
    .map((l) => l.text)
    .join('\n')
}
