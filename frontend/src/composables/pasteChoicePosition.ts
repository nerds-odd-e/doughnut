/** Viewport-relative geometry (from `getBoundingClientRect()` or Quill's `getBounds()`,
 * both already viewport-relative) of what a paste just inserted. */
export type PasteChoiceAnchorRect = {
  top: number
  bottom: number
  left: number
  right: number
}

/** Picks the four edges an anchor needs from any DOMRect-shaped geometry
 * (`getBoundingClientRect()`, Quill's `getBounds()`) — both already viewport-relative. */
export function toPasteChoiceAnchorRect(rect: {
  top: number
  bottom: number
  left: number
  right: number
}): PasteChoiceAnchorRect {
  return {
    top: rect.top,
    bottom: rect.bottom,
    left: rect.left,
    right: rect.right,
  }
}

const GAP_PX = 4
const VIEWPORT_MARGIN_PX = 8

/** Computes a `position: fixed` `top`/`left` for the paste-choice action bar: placed
 * just below `anchor` (the just-pasted content's own geometry), flipped above when it
 * would not fit within the viewport, and clamped horizontally within a viewport margin.
 * Placing it outside `anchor`'s own vertical span, rather than on top of it, is what
 * keeps it from covering the inserted content. */
export function computePasteChoiceStyle(
  anchor: PasteChoiceAnchorRect,
  barSize: { width: number; height: number },
  viewport: { width: number; height: number }
): { top: string; left: string } {
  const fitsBelow =
    anchor.bottom + GAP_PX + barSize.height <=
    viewport.height - VIEWPORT_MARGIN_PX
  const top = fitsBelow
    ? anchor.bottom + GAP_PX
    : Math.max(VIEWPORT_MARGIN_PX, anchor.top - GAP_PX - barSize.height)

  const maxLeft = Math.max(
    VIEWPORT_MARGIN_PX,
    viewport.width - VIEWPORT_MARGIN_PX - barSize.width
  )
  const left = Math.min(Math.max(anchor.left, VIEWPORT_MARGIN_PX), maxLeft)

  return { top: `${top}px`, left: `${left}px` }
}
