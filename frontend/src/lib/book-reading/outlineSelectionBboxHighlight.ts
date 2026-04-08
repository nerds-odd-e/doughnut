import {
  PDF_OUTLINE_V1_NORMALIZED_MAX,
  type PdfOutlineV1Bbox,
} from "./pdfOutlineV1Anchor"

export const OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS = 2000

export function attachOutlineSelectionBboxHighlight(
  pageLayer: HTMLElement,
  viewportWidth: number,
  viewportHeight: number,
  bbox: PdfOutlineV1Bbox
): () => void {
  const [x0, y0, x1, y1] = bbox
  const scale = PDF_OUTLINE_V1_NORMALIZED_MAX
  const overlay = document.createElement("div")
  overlay.dataset.testid = "outline-selection-bbox-highlight"
  overlay.style.position = "absolute"
  overlay.style.left = `${(x0 / scale) * viewportWidth}px`
  overlay.style.top = `${(y0 / scale) * viewportHeight}px`
  overlay.style.width = `${((x1 - x0) / scale) * viewportWidth}px`
  overlay.style.height = `${((y1 - y0) / scale) * viewportHeight}px`
  overlay.style.backgroundColor = "rgba(255, 0, 0, 0.3)"
  overlay.style.pointerEvents = "none"
  overlay.style.zIndex = "100"
  overlay.style.opacity = "1"
  overlay.style.transition = `opacity ${OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS}ms ease-out`

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
  }, OUTLINE_SELECTION_BBOX_HIGHLIGHT_FADE_MS)

  return cancel
}
