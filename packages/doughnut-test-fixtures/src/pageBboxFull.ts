import type { PdfLocatorFull } from '@generated/doughnut-backend-api'

/** Page anchor without bbox (tests mirror `GET …/book` page-only heading shape). */
export function pageBboxPageIndexOnly(pageIndex: number): PdfLocatorFull {
  return { type: 'PdfLocator_Full', pageIndex } as PdfLocatorFull
}

export function pageBboxWithNormalizedBbox(
  pageIndex: number,
  bbox: ReadonlyArray<number>
): PdfLocatorFull {
  return { type: 'PdfLocator_Full', pageIndex, bbox: [...bbox] }
}
