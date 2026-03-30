import stringWidth from 'string-width'

export function resolvedTerminalWidth(): number {
  const c = process.stdout.columns
  return typeof c === 'number' && c >= 40 ? c : 120
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

/** One logical row per wrapped segment; `itemIndex` groups continuation lines for one choice. */
export function numberedTerminalListLines(
  items: readonly string[],
  width: number
): NumberedTerminalListLine[] {
  const out: NumberedTerminalListLine[] = []
  const contIndent = '   '
  const contW = stringWidth(contIndent)
  for (let i = 0; i < items.length; i++) {
    const prefix = `${i + 1}. `
    const pw = stringWidth(prefix)
    const firstBudget = Math.max(1, width - pw)
    const nextBudget = Math.max(1, width - contW)
    const wrapped = wrapParagraphToWidths(items[i]!, firstBudget, nextBudget)
    out.push({ itemIndex: i, text: prefix + (wrapped[0] ?? '') })
    for (let w = 1; w < wrapped.length; w++) {
      out.push({ itemIndex: i, text: contIndent + wrapped[w]! })
    }
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
