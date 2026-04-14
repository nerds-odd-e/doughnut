const HOLD_THRESHOLD_MS = 500
const HOLD_MOVE_TOLERANCE_PX = 10
const STRUCTURAL_TITLE_MAX_CHARS = 512

export function attachBookBlockSelectionBboxHighlight(
  pageLayer: HTMLElement,
  rect: { left: number; top: number; width: number; height: number },
  contentBlockId?: number,
  onLongPress?: (
    contentBlockId: number,
    clientX: number,
    clientY: number,
    derivedTitle: string | undefined
  ) => void,
  derivedTitle?: string
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
  if (
    derivedTitle !== undefined &&
    derivedTitle.length >= STRUCTURAL_TITLE_MAX_CHARS
  ) {
    overlay.dataset.derivedTitleTruncated = "true"
  }

  let holdTimer: ReturnType<typeof setTimeout> | null = null
  let startX = 0
  let startY = 0

  function cancelTimer() {
    if (holdTimer !== null) {
      clearTimeout(holdTimer)
      holdTimer = null
    }
  }

  if (contentBlockId !== undefined && onLongPress) {
    const id = contentBlockId
    const cb = onLongPress
    const title = derivedTitle
    overlay.addEventListener("pointerdown", (e: PointerEvent) => {
      startX = e.clientX
      startY = e.clientY
      cancelTimer()
      holdTimer = setTimeout(() => {
        holdTimer = null
        cb(id, e.clientX, e.clientY, title)
      }, HOLD_THRESHOLD_MS)
    })
    overlay.addEventListener("pointermove", (e: PointerEvent) => {
      if (holdTimer === null) return
      if (
        Math.hypot(e.clientX - startX, e.clientY - startY) >
        HOLD_MOVE_TOLERANCE_PX
      ) {
        cancelTimer()
      }
    })
    overlay.addEventListener("pointerup", cancelTimer)
    overlay.addEventListener("pointercancel", cancelTimer)
  }

  pageLayer.appendChild(overlay)

  return () => {
    cancelTimer()
    overlay.remove()
  }
}
