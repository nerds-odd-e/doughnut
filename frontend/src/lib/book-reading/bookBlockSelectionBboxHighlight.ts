const STRUCTURAL_TITLE_MAX_CHARS = 512
export const BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS = 2000

type PixelRect = { left: number; top: number; width: number; height: number }

export type AttachBookBlockSelectionBboxHighlightOptions = PixelRect & {
  contentBlockId?: number
  derivedTitle?: string
}

export function attachBookBlockSelectionBboxHighlight(
  pageLayer: HTMLElement,
  options: AttachBookBlockSelectionBboxHighlightOptions
): () => void {
  const { left, top, width, height, contentBlockId, derivedTitle } = options
  const overlay = document.createElement("div")
  overlay.dataset.testid = "book-block-selection-bbox-highlight"
  overlay.style.position = "absolute"
  overlay.style.left = `${left}px`
  overlay.style.top = `${top}px`
  overlay.style.width = `${width}px`
  overlay.style.height = `${height}px`
  overlay.style.backgroundColor = "rgba(255, 0, 0, 0.3)"
  overlay.style.pointerEvents = "none"
  overlay.style.zIndex = "100"
  if (contentBlockId !== undefined) {
    overlay.dataset.bookContentBlockId = String(contentBlockId)
  }
  if (
    derivedTitle !== undefined &&
    derivedTitle.length >= STRUCTURAL_TITLE_MAX_CHARS
  ) {
    overlay.dataset.derivedTitleTruncated = "true"
  }

  pageLayer.appendChild(overlay)

  overlay.style.opacity = "1"
  overlay.style.transition = `opacity ${BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS}ms ease-out`
  let fadeStartTimer: ReturnType<typeof setTimeout> | null = setTimeout(() => {
    fadeStartTimer = null
    overlay.style.opacity = "0"
  }, 0)
  let removalTimer: ReturnType<typeof setTimeout> | null = setTimeout(() => {
    removalTimer = null
    overlay.remove()
  }, BOOK_BLOCK_SELECTION_BBOX_HIGHLIGHT_FADE_MS)

  return () => {
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
}
