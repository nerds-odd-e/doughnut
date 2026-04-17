import { debounce } from "es-toolkit"
import type {
  ContentLocatorFull,
  EpubLocatorFull,
  PdfLocatorFull,
} from "@generated/doughnut-backend-api"

export type LastReadPositionPatchBody = {
  locator: ContentLocatorFull
  selectedBookBlockId?: number
}

export type LastReadPositionPatchDebouncer = {
  propose: (locator: ContentLocatorFull, selectedBookBlockId?: number) => void
  flush: () => void
  cancel: () => void
}

function sameLocator(a: ContentLocatorFull, b: ContentLocatorFull): boolean {
  if (a.type !== b.type) {
    return false
  }
  if (a.type === "EpubLocator_Full") {
    const ea = a as EpubLocatorFull
    const eb = b as EpubLocatorFull
    const fa = ea.fragment?.trim() ?? ""
    const fb = eb.fragment?.trim() ?? ""
    return ea.href === eb.href && fa === fb
  }
  if (a.type === "PdfLocator_Full") {
    const pa = a as PdfLocatorFull
    const pb = b as PdfLocatorFull
    if (pa.pageIndex !== pb.pageIndex) {
      return false
    }
    if (pa.bbox.length !== pb.bbox.length) {
      return false
    }
    for (let i = 0; i < pa.bbox.length; i++) {
      if (pa.bbox[i] !== pb.bbox[i]) {
        return false
      }
    }
    return true
  }
  return false
}

function sameBody(a: LastReadPositionPatchBody, b: LastReadPositionPatchBody) {
  if (a.selectedBookBlockId !== b.selectedBookBlockId) {
    return false
  }
  return sameLocator(a.locator, b.locator)
}

export function createLastReadPositionPatchDebouncer(options: {
  delayMs: number
  patch: (body: LastReadPositionPatchBody) => Promise<unknown>
}): LastReadPositionPatchDebouncer {
  const { delayMs, patch } = options
  let lastSent: LastReadPositionPatchBody | null = null

  const sendIfNeeded = (next: LastReadPositionPatchBody) => {
    if (lastSent !== null && sameBody(lastSent, next)) {
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
    propose(locator: ContentLocatorFull, selectedBookBlockId?: number) {
      const next: LastReadPositionPatchBody =
        selectedBookBlockId === undefined
          ? { locator }
          : { locator, selectedBookBlockId }
      debounced(next)
    },
    flush() {
      debounced.flush()
    },
    cancel() {
      debounced.cancel()
    },
  }
}
