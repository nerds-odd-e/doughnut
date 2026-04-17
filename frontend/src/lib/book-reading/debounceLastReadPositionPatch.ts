import { debounce } from "es-toolkit"

export type LastReadPositionPdfBody = {
  pageIndex: number
  normalizedY: number
  selectedBookBlockId?: number
}

export type LastReadPositionEpubBody = {
  epubLocator: string
  selectedBookBlockId?: number
}

export type LastReadPositionPatchBody =
  | LastReadPositionPdfBody
  | LastReadPositionEpubBody

export type LastReadPositionPatchDebouncer = {
  propose: (
    pageIndex: number,
    normalizedY: number,
    selectedBookBlockId?: number
  ) => void
  proposeEpubLocator: (
    epubLocator: string,
    selectedBookBlockId?: number
  ) => void
  cancel: () => void
}

function isEpubBody(
  body: LastReadPositionPatchBody
): body is LastReadPositionEpubBody {
  return "epubLocator" in body
}

export function createLastReadPositionPatchDebouncer(options: {
  delayMs: number
  patch: (body: LastReadPositionPatchBody) => Promise<unknown>
}): LastReadPositionPatchDebouncer {
  const { delayMs, patch } = options
  let lastSent: LastReadPositionPatchBody | null = null

  function same(a: LastReadPositionPatchBody, b: LastReadPositionPatchBody) {
    if (isEpubBody(a) !== isEpubBody(b)) {
      return false
    }
    if (isEpubBody(a) && isEpubBody(b)) {
      return (
        a.epubLocator === b.epubLocator &&
        a.selectedBookBlockId === b.selectedBookBlockId
      )
    }
    const pdfA = a as LastReadPositionPdfBody
    const pdfB = b as LastReadPositionPdfBody
    if (
      pdfA.pageIndex !== pdfB.pageIndex ||
      pdfA.normalizedY !== pdfB.normalizedY
    ) {
      return false
    }
    return pdfA.selectedBookBlockId === pdfB.selectedBookBlockId
  }

  const sendIfNeeded = (next: LastReadPositionPatchBody) => {
    if (lastSent !== null && same(lastSent, next)) {
      return
    }
    patch(next)
      .then(() => {
        lastSent = next
      })
      .catch(() => undefined)
  }

  const debounced = debounce((next: LastReadPositionPatchBody) => {
    sendIfNeeded(next)
  }, delayMs)

  return {
    propose(
      pageIndex: number,
      normalizedY: number,
      selectedBookBlockId?: number
    ) {
      const next: LastReadPositionPdfBody =
        selectedBookBlockId === undefined
          ? { pageIndex, normalizedY }
          : { pageIndex, normalizedY, selectedBookBlockId }
      debounced(next)
    },
    proposeEpubLocator(epubLocator: string, selectedBookBlockId?: number) {
      const next: LastReadPositionEpubBody =
        selectedBookBlockId === undefined
          ? { epubLocator }
          : { epubLocator, selectedBookBlockId }
      debounced(next)
    },
    cancel() {
      debounced.cancel()
    },
  }
}
