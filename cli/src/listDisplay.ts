import { GREY, RESET, REVERSE } from './ansi.js'

/** Max visible lines in Current guidance scrollable lists (commands, tokens, MCQ). */
export const CURRENT_GUIDANCE_MAX_VISIBLE = 8

/**
 * Scrollable list with fixed-height window. Indicators replace option slots, never add lines.
 *
 * When `itemIndexPerLine` is provided, `highlightIndex` is a logical item index:
 * all physical lines belonging to that item are highlighted, and scroll anchors on
 * the first line of the item.
 */
export function formatHighlightedList(
  lines: readonly string[],
  maxVisible = CURRENT_GUIDANCE_MAX_VISIBLE,
  highlightIndex = 0,
  itemIndexPerLine?: readonly number[]
): string[] {
  const total = lines.length
  if (total === 0) return []

  const anchorLine = itemIndexPerLine
    ? itemIndexPerLine.indexOf(highlightIndex)
    : highlightIndex
  const isHighlightedLine = itemIndexPerLine
    ? (idx: number) => itemIndexPerLine[idx] === highlightIndex
    : (idx: number) => idx === anchorLine

  const isOverflowing = total > maxVisible
  if (!isOverflowing) {
    return lines.map((line, i) =>
      isHighlightedLine(i)
        ? `${REVERSE}${line}${RESET}`
        : `${GREY}${line}${RESET}`
    )
  }

  const scrollOffset = computeScrollOffset(total, maxVisible, anchorLine)
  const window = lines.slice(scrollOffset, scrollOffset + maxVisible)
  const showMoreAbove = shouldShowMoreAbove(
    scrollOffset,
    anchorLine,
    maxVisible,
    total
  )
  const showMoreBelow = shouldShowMoreBelow(
    scrollOffset,
    anchorLine,
    maxVisible,
    total
  )

  const firstOptionIndex = showMoreAbove ? 1 : 0
  const lastOptionIndex = showMoreBelow ? window.length - 2 : window.length - 1

  const result: string[] = []
  if (showMoreAbove) result.push(`${GREY}  ↑ more above${RESET}`)
  for (let i = firstOptionIndex; i <= lastOptionIndex; i++) {
    const line = window[i]!
    result.push(
      isHighlightedLine(scrollOffset + i)
        ? `${REVERSE}${line}${RESET}`
        : `${GREY}${line}${RESET}`
    )
  }
  if (showMoreBelow) result.push(`${GREY}  ↓ more below${RESET}`)
  return result
}

function computeScrollOffset(
  total: number,
  maxVisible: number,
  highlightIndex: number
): number {
  const maxScroll = total - maxVisible
  const hasMoreBelow = (offset: number) => offset + maxVisible < total

  if (!hasMoreBelow(0)) return 0

  const optionSlotsWithMoreBelow = maxVisible - 1
  const lastVisibleWithMoreBelow = optionSlotsWithMoreBelow - 1

  if (highlightIndex <= lastVisibleWithMoreBelow) return 0

  if (highlightIndex === optionSlotsWithMoreBelow) return 1

  let offset = Math.max(0, Math.min(highlightIndex - maxVisible + 1, maxScroll))

  if (offset > 0 && hasMoreBelow(offset)) {
    const optionSlotsWithBoth = maxVisible - 2
    if (highlightIndex < offset + 1) {
      offset = Math.max(0, highlightIndex - 1)
    } else if (highlightIndex > offset + optionSlotsWithBoth) {
      offset = Math.min(maxScroll, highlightIndex - optionSlotsWithBoth)
    }
  }
  return offset
}

function shouldShowMoreAbove(
  scrollOffset: number,
  highlightIndex: number,
  maxVisible: number,
  total: number
): boolean {
  if (scrollOffset > 0) return true
  const hasMoreBelow = scrollOffset + maxVisible < total
  const lastVisibleBeforeScroll = maxVisible - 2
  return hasMoreBelow && highlightIndex === lastVisibleBeforeScroll
}

function shouldShowMoreBelow(
  scrollOffset: number,
  highlightIndex: number,
  maxVisible: number,
  total: number
): boolean {
  const hasMoreBelow = scrollOffset + maxVisible < total
  const firstHiddenByMoreBelow = scrollOffset + maxVisible - 1
  return hasMoreBelow && highlightIndex < firstHiddenByMoreBelow
}
