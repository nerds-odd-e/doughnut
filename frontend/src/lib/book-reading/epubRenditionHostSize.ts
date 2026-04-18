/**
 * Dimensions passed to epub.js `rendition.resize()`.
 *
 * The inner `.epub-container` uses overflow scroll; its `clientWidth` excludes the scrollbar
 * gutter. Measuring it produced a narrower width than `renderTo(host)` used, which triggered a
 * spurious `resize()` → `clear()` → `redisplay()` and wrong spine position. Always measure the
 * same host element given to `renderTo()`.
 */
export function epubRenditionResizeDimensions(
  host: HTMLElement
): { width: number; height: number } | null {
  const width = Math.floor(host.clientWidth)
  const height = Math.floor(host.clientHeight)
  if (width < 1 || height < 1) {
    return null
  }
  return { width, height }
}
