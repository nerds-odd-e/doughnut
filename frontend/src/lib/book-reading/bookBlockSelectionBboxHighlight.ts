export function attachBookBlockSelectionBboxHighlight(
  pageLayer: HTMLElement,
  rect: { left: number; top: number; width: number; height: number },
  contentBlockId?: number
): () => void {
  const overlay = document.createElement("div")
  overlay.dataset.testid = "book-block-selection-bbox-highlight"
  overlay.style.position = "absolute"
  overlay.style.left = `${rect.left}px`
  overlay.style.top = `${rect.top}px`
  overlay.style.width = `${rect.width}px`
  overlay.style.height = `${rect.height}px`
  overlay.style.backgroundColor = "rgba(255, 0, 0, 0.3)"
  overlay.style.pointerEvents = "auto"
  overlay.style.zIndex = "100"
  if (contentBlockId !== undefined) {
    overlay.dataset.bookContentBlockId = String(contentBlockId)
  }

  pageLayer.appendChild(overlay)

  return () => {
    overlay.remove()
  }
}
