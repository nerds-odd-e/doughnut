export const MAX_COMFORTABLE_PDF_WIDTH_PX = 960

/**
 * After pdf.js has applied its own `"page-width"` scale (container minus padding, page chrome, etc.),
 * clamp so the rendered page is not wider than MAX_COMFORTABLE_PDF_WIDTH_PX at intrinsic width.
 */
export function pdfScaleAfterPageWidth(
  pdfJsPageWidthScale: number,
  intrinsicPageWidth: number
): number {
  if (
    intrinsicPageWidth <= 0 ||
    !Number.isFinite(pdfJsPageWidthScale) ||
    pdfJsPageWidthScale <= 0
  ) {
    return 1
  }
  const capScale = MAX_COMFORTABLE_PDF_WIDTH_PX / intrinsicPageWidth
  return Math.min(pdfJsPageWidthScale, capScale)
}
