export const BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS = 2000

export function attachBookBlockSelectionBboxHighlight(
  pageLayer: HTMLElement,
  rect: { left: number; top: number; width: number; height: number }
): () => void {
  const overlay = document.createElement("div")
  overlay.dataset.testid = "book-block-selection-bbox-highlight"
  overlay.style.position = "absolute"
  overlay.style.left = `${rect.left}px`
  overlay.style.top = `${rect.top}px`
  overlay.style.width = `${rect.width}px`
  overlay.style.height = `${rect.height}px`
  overlay.style.backgroundColor = "rgba(255, 0, 0, 0.3)"
  overlay.style.pointerEvents = "none"
  overlay.style.zIndex = "100"
  overlay.style.opacity = "1"
  overlay.style.transition = `opacity ${BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS}ms ease-out`

  let fadeStartTimer: ReturnType<typeof setTimeout> | null = null
  let removalTimer: ReturnType<typeof setTimeout> | null = null

  const cancel = () => {
    if (fadeStartTimer !== null) {
      clearTimeout(fadeStartTimer)
      fadeStartTimer = null
    }
    if (removalTimer !== null) {
      clearTimeout(removalTimer)
      removalTimer = null
    }
    overlay.remove()
  }

  pageLayer.appendChild(overlay)
  fadeStartTimer = setTimeout(() => {
    fadeStartTimer = null
    overlay.style.opacity = "0"
  }, 0)
  removalTimer = setTimeout(() => {
    removalTimer = null
    overlay.remove()
  }, BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS)

  return cancel
}
