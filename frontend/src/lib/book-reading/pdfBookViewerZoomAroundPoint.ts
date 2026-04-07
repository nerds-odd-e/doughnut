/**
 * Scroll offsets after uniform scaling of scrollable content from its top-left corner,
 * keeping the point under (originXInContainer, originYInContainer) fixed in the viewport.
 */
export function scrollAfterUniformContentScale(options: {
  scrollLeft: number
  scrollTop: number
  originXInContainer: number
  originYInContainer: number
  scaleFactor: number
}): { scrollLeft: number; scrollTop: number } {
  const f = options.scaleFactor
  return {
    scrollLeft:
      (options.scrollLeft + options.originXInContainer) * f -
      options.originXInContainer,
    scrollTop:
      (options.scrollTop + options.originYInContainer) * f -
      options.originYInContainer,
  }
}

export function clampScrollAxis(
  value: number,
  scrollSize: number,
  clientSize: number
): number {
  const max = Math.max(0, scrollSize - clientSize)
  return Math.min(max, Math.max(0, value))
}

/** Maps ctrl/meta wheel deltaY (pixels, typical trackpad) to a per-event scale multiplier. */
export function wheelDeltaYToScaleFactor(deltaY: number): number {
  const factor = Math.exp(-deltaY * 0.002)
  return Math.min(1.12, Math.max(0.88, factor))
}
