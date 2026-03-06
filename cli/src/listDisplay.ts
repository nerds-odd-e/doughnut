const GREY = '\x1b[90m'
const REVERSE = '\x1b[7m'
const RESET = '\x1b[0m'

export function formatHighlightedList(
  lines: string[],
  maxVisible = 8,
  highlightIndex = 0
): string[] {
  if (lines.length === 0) return []
  const total = lines.length
  const scrollOffset =
    total <= maxVisible
      ? 0
      : Math.max(
          0,
          Math.min(highlightIndex - maxVisible + 1, total - maxVisible)
        )
  const visibleLines = lines.slice(scrollOffset, scrollOffset + maxVisible)
  const result: string[] = []
  if (scrollOffset > 0) result.push(`${GREY}  ↑ more above${RESET}`)
  const highlightPos = highlightIndex - scrollOffset
  for (let i = 0; i < visibleLines.length; i++) {
    result.push(
      i === highlightPos
        ? `${REVERSE}${visibleLines[i]}${RESET}`
        : `${GREY}${visibleLines[i]}${RESET}`
    )
  }
  if (scrollOffset + visibleLines.length < total) {
    result.push(`${GREY}  ↓ more below${RESET}`)
  }
  return result
}
