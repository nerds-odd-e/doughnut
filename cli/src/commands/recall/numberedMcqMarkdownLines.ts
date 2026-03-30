import stringWidth from 'string-width'
import { renderMarkdownToTerminal } from '../../markdown.js'
import type { NumberedTerminalListLine } from '../../terminalColumns.js'

const CONT_INDENT = '   '

export function numberedMcqMarkdownLinesForTerminal(
  items: readonly string[],
  width: number
): NumberedTerminalListLine[] {
  const out: NumberedTerminalListLine[] = []
  for (let i = 0; i < items.length; i++) {
    const prefix = `${i + 1}. `
    const pw = stringWidth(prefix)
    const firstBudget = Math.max(1, width - pw)
    const rendered = renderMarkdownToTerminal(items[i]!, firstBudget)
    const segments = rendered.length > 0 ? rendered.split('\n') : ['']
    const first = segments[0]!
    out.push({
      itemIndex: i,
      text: prefix + (first.length > 0 ? first : ''),
    })
    for (let w = 1; w < segments.length; w++) {
      const seg = segments[w]!
      out.push({
        itemIndex: i,
        text: CONT_INDENT + (seg.length > 0 ? seg : ' '),
      })
    }
  }
  return out
}
