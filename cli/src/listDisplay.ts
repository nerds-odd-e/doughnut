import { GREY, RESET, REVERSE } from './ansi.js'

/** Max visible lines in Current guidance scrollable lists (commands, tokens, MCQ). */
export const CURRENT_GUIDANCE_MAX_VISIBLE = 8

/** Scrollable list with fixed-height window. Indicators replace option slots, never add lines. */
export function formatHighlightedList(
  lines: string[],
  maxVisible = CURRENT_GUIDANCE_MAX_VISIBLE,
  highlightIndex = 0
): string[] {
  const total = lines.length
  if (total === 0) return []

  const isOverflowing = total > maxVisible
  if (!isOverflowing) {
    return lines.map((line, i) =>
      i === highlightIndex
        ? `${REVERSE}${line}${RESET}`
        : `${GREY}${line}${RESET}`
    )
  }

  const scrollOffset = computeScrollOffset(total, maxVisible, highlightIndex)
  const window = lines.slice(scrollOffset, scrollOffset + maxVisible)
  const showMoreAbove = shouldShowMoreAbove(
    scrollOffset,
    highlightIndex,
    maxVisible,
    total
  )
  const showMoreBelow = shouldShowMoreBelow(
    scrollOffset,
    highlightIndex,
    maxVisible,
    total
  )

  const firstOptionIndex = showMoreAbove ? 1 : 0
  const lastOptionIndex = showMoreBelow ? window.length - 2 : window.length - 1
  const highlightPosInWindow = highlightIndex - scrollOffset

  const result: string[] = []
  if (showMoreAbove) result.push(`${GREY}  ↑ more above${RESET}`)
  for (let i = firstOptionIndex; i <= lastOptionIndex; i++) {
    const line = window[i]
    const isHighlighted = i === highlightPosInWindow
    result.push(
      isHighlighted ? `${REVERSE}${line}${RESET}` : `${GREY}${line}${RESET}`
    )
  }
  if (showMoreBelow) result.push(`${GREY}  ↓ more below${RESET}`)
  return result
}

/**
 * Like {@link formatHighlightedList}, but each physical line belongs to a logical item (e.g. MCQ choice).
 * Scroll anchoring uses the first line of `selectedItemIndex`; all lines for that item are highlighted.
 */
export function formatHighlightedListByItem(
  lines: string[],
  itemIndexPerLine: readonly number[],
  maxVisible = CURRENT_GUIDANCE_MAX_VISIBLE,
  selectedItemIndex = 0
): string[] {
  const total = lines.length
  if (total === 0) return []

  let anchorLine = 0
  for (let i = 0; i < total; i++) {
    if (itemIndexPerLine[i] === selectedItemIndex) {
      anchorLine = i
      break
    }
  }

  const isOverflowing = total > maxVisible
  if (!isOverflowing) {
    return lines.map((line, i) =>
      itemIndexPerLine[i] === selectedItemIndex
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
    const actualLineIndex = scrollOffset + i
    const isHighlighted =
      itemIndexPerLine[actualLineIndex] === selectedItemIndex
    result.push(
      isHighlighted ? `${REVERSE}${line}${RESET}` : `${GREY}${line}${RESET}`
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
