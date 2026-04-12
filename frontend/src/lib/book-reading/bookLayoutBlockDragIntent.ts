export type BookLayoutBlockDragIntent = "INDENT" | "OUTDENT" | "NONE"

/** Minimum horizontal movement (px) before a drag can count as indent/outdent. */
export const BOOK_LAYOUT_BLOCK_DRAG_THRESHOLD_PX = 24

export type BookLayoutBlockDragIntentOptions = {
  thresholdPx: number
}

/**
 * Classifies a pointer drag as indent (right), outdent (left), or none.
 * Requires horizontal movement to meet the threshold and exceed vertical movement
 * so scrolling the layout aside does not trigger depth changes.
 */
export function bookLayoutBlockDragIntent(
  dx: number,
  dy: number,
  options: BookLayoutBlockDragIntentOptions
): BookLayoutBlockDragIntent {
  const { thresholdPx } = options
  if (Math.abs(dx) < thresholdPx || Math.abs(dx) <= Math.abs(dy)) {
    return "NONE"
  }
  if (dx > 0) {
    return "INDENT"
  }
  if (dx < 0) {
    return "OUTDENT"
  }
  return "NONE"
}

/**
 * True once the user has moved far enough horizontally (and not vertically-dominant)
 * to treat the gesture as a horizontal drag — use for pointer capture.
 */
export function bookLayoutBlockDragShouldCapture(
  dx: number,
  dy: number,
  options: BookLayoutBlockDragIntentOptions
): boolean {
  const { thresholdPx } = options
  return Math.abs(dx) >= thresholdPx && Math.abs(dx) > Math.abs(dy)
}
