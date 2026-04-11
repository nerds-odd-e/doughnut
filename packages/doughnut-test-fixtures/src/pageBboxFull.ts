import type { PageBboxFull } from '@generated/doughnut-backend-api'

export function pageBboxPageIndexOnly(pageIndex: number): PageBboxFull {
  return { pageIndex } as PageBboxFull
}

export function pageBboxWithNormalizedBbox(
  pageIndex: number,
  bbox: ReadonlyArray<number>
): PageBboxFull {
  return { pageIndex, bbox: [...bbox] }
}
