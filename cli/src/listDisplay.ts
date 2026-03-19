import { GREY, RESET, REVERSE } from './ansi.js'

export function formatHighlightedList(
  lines: string[],
  maxVisible = 8,
  highlightIndex = 0
): string[] {
  if (lines.length === 0) return []
  const total = lines.length
  const hasMoreAbove = total > maxVisible
  const hasMoreBelow = total > maxVisible

  const hasMoreContentBelow = (offset: number) => offset + maxVisible < total
  const optionSlotsWhenMoreBelow = maxVisible - 1

  let scrollOffset: number
  if (total <= maxVisible) {
    scrollOffset = 0
  } else if (highlightIndex < maxVisible - 1 && hasMoreContentBelow(0)) {
    scrollOffset = 0
  } else if (highlightIndex === maxVisible - 1 && hasMoreContentBelow(0)) {
    scrollOffset = 1
  } else {
    scrollOffset = Math.max(
      0,
      Math.min(highlightIndex - maxVisible + 1, total - maxVisible)
    )
  }

  if (scrollOffset > 0 && hasMoreContentBelow(scrollOffset)) {
    const optionSlots = maxVisible - 2
    if (highlightIndex < scrollOffset + 1) {
      scrollOffset = Math.max(0, highlightIndex - 1)
    } else if (highlightIndex > scrollOffset + optionSlots) {
      scrollOffset = Math.min(
        total - maxVisible,
        Math.max(scrollOffset, highlightIndex - optionSlots)
      )
    }
  }

  const finalVisibleLines = lines.slice(scrollOffset, scrollOffset + maxVisible)
  const finalShowMoreAbove =
    scrollOffset > 0 ||
    (scrollOffset === 0 &&
      highlightIndex === maxVisible - 2 &&
      hasMoreContentBelow(0))
  const finalShowMoreBelow =
    hasMoreContentBelow(scrollOffset) &&
    highlightIndex < scrollOffset + optionSlotsWhenMoreBelow

  const moreAboveLine = `${GREY}  ↑ more above${RESET}`
  const moreBelowLine = `${GREY}  ↓ more below${RESET}`

  if (!(hasMoreAbove || hasMoreBelow)) {
    const result: string[] = []
    for (let i = 0; i < finalVisibleLines.length; i++) {
      result.push(
        i === highlightIndex
          ? `${REVERSE}${finalVisibleLines[i]}${RESET}`
          : `${GREY}${finalVisibleLines[i]}${RESET}`
      )
    }
    return result
  }

  const result: string[] = []
  let start = 0
  let end = finalVisibleLines.length
  if (finalShowMoreAbove) {
    result.push(moreAboveLine)
    start = 1
  }
  if (finalShowMoreBelow) end -= 1

  const highlightPos = highlightIndex - scrollOffset
  for (let i = start; i < end; i++) {
    result.push(
      i === highlightPos
        ? `${REVERSE}${finalVisibleLines[i]}${RESET}`
        : `${GREY}${finalVisibleLines[i]}${RESET}`
    )
  }
  if (finalShowMoreBelow) result.push(moreBelowLine)
  return result
}
