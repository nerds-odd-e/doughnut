import type { ViewerLocatorRect } from "@/composables/bookReaderViewerRef"
import { asEpubLocator } from "@/lib/book-reading/asEpubLocator"
import type { EpubViewBlockStarts } from "@/lib/book-reading/currentBlockIdFromEpubView"
import {
  epubSpinePathMatches,
  splitEpubHref,
} from "@/lib/book-reading/epubHrefMatch"
import type {
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/donut-backend-api"
import type { Book as EpubJsBook, Rendition } from "epubjs"
import type { Ref } from "vue"

const READING_PANEL_ANCHOR_GAP_PX = 8

type EpubRenditionIframeView = {
  displayed?: boolean
  section?: { href?: string; index?: number }
  contents?: { document?: Document }
  iframe?: HTMLIFrameElement
}

type EpubSpineItem = { href?: string; index?: number }

export function epubSpineItems(
  b: EpubJsBook | null
): ReadonlyArray<EpubSpineItem> {
  const raw = (
    b as unknown as { spine?: { spineItems?: ReadonlyArray<EpubSpineItem> } }
  )?.spine?.spineItems
  return Array.isArray(raw) ? raw : []
}

function forEachRenditionView(
  r: Rendition,
  fn: (view: EpubRenditionIframeView) => void
): void {
  const views = r.views() as unknown
  if (
    views &&
    typeof views === "object" &&
    typeof (views as { forEach?: unknown }).forEach === "function"
  ) {
    ;(
      views as { forEach: (cb: (v: EpubRenditionIframeView) => void) => void }
    ).forEach(fn)
  }
}

function resolveEpubLocatorElement(
  r: Rendition,
  epub: EpubLocatorFull
): HTMLElement | null {
  const storedPath = splitEpubHref(epub.href.trim()).path
  if (storedPath.length === 0) {
    return null
  }
  const frag = epub.fragment?.trim() ?? null

  const matches: EpubRenditionIframeView[] = []
  forEachRenditionView(r, (view) => {
    if (!view.displayed || !view.section?.href) {
      return
    }
    const { path: viewPath } = splitEpubHref(view.section.href.trim())
    if (epubSpinePathMatches(storedPath, viewPath)) {
      matches.push(view)
    }
  })

  const hit = matches.length > 0 ? (matches[matches.length - 1] ?? null) : null
  const doc = hit?.contents?.document
  if (!doc?.body) {
    return null
  }
  if (frag !== null && frag.length > 0) {
    const byId = doc.getElementById(frag)
    if (byId) {
      return byId
    }
  }
  return doc.body
}

/**
 * Distance in px from the top of the reader's view down to the locator's start (negative when
 * above). A start in a section that is not rendered lies wholly above (−∞) or below (+∞) the
 * rendered sections; null when it cannot be placed.
 */
function startTopPx(
  container: Element,
  rendered: readonly EpubRenditionIframeView[],
  spine: ReadonlyArray<EpubSpineItem>,
  epub: EpubLocatorFull
): number | null {
  const storedPath = splitEpubHref(epub.href.trim()).path
  const index = spine.find(
    (s) => s.href !== undefined && epubSpinePathMatches(storedPath, s.href)
  )?.index
  if (index === undefined || rendered.length === 0) {
    return null
  }
  const view = rendered.find((v) => v.section?.index === index)
  if (!view?.iframe) {
    return index < (rendered[0]?.section?.index ?? 0)
      ? Number.NEGATIVE_INFINITY
      : Number.POSITIVE_INFINITY
  }
  const frag = epub.fragment?.trim() ?? ""
  const el =
    frag.length > 0 ? view.contents?.document?.getElementById(frag) : null
  return Math.round(
    view.iframe.getBoundingClientRect().top +
      (el ? el.getBoundingClientRect().top : 0) -
      container.getBoundingClientRect().top
  )
}

export function useEpubLocatorGeometry(opts: {
  hostRef: Ref<HTMLElement | null>
  getRendition: () => Rendition | null
  getBook: () => EpubJsBook | null
}) {
  /**
   * Where block starts lie relative to the top of the reader's view right now. The landing
   * limit is how far below the top a start may lie and still count as landed at the top:
   * 0, or the whole view once it is scrolled to the end of the book and cannot go further.
   */
  function viewBlockStarts(): EpubViewBlockStarts | null {
    const container = opts.hostRef.value?.querySelector(".epub-container")
    const r = opts.getRendition()
    if (!container || !r) {
      return null
    }
    const spine = epubSpineItems(opts.getBook())
    const rendered: EpubRenditionIframeView[] = []
    let lastRenderedIndex = -1
    forEachRenditionView(r, (view) => {
      lastRenderedIndex = Math.max(lastRenderedIndex, view.section?.index ?? -1)
      if (view.displayed && view.section?.index !== undefined) {
        rendered.push(view)
      }
    })
    const atEnd =
      lastRenderedIndex === spine.length - 1 &&
      container.scrollTop + container.clientHeight >= container.scrollHeight - 1
    return {
      startTopPx: (start) => startTopPx(container, rendered, spine, start),
      landingLimitPx: atEnd ? container.clientHeight : 0,
    }
  }

  function resolveLocatorRect(
    locator: ContentLocatorFull
  ): ViewerLocatorRect | null {
    const r = opts.getRendition()
    if (!opts.hostRef.value || !r) {
      return null
    }
    const epub = asEpubLocator(locator)
    if (!epub) {
      return null
    }
    const el = resolveEpubLocatorElement(r, epub)
    if (!el) {
      return null
    }
    const b = el.getBoundingClientRect()
    return {
      top: b.top,
      bottom: b.bottom,
      left: b.left,
      right: b.right,
      width: Math.max(0, b.width),
      height: Math.max(0, b.height),
    }
  }

  function isLocatorBottomVisible(
    locator: ContentLocatorFull,
    obstructionPx: number
  ): boolean {
    const host = opts.hostRef.value
    if (!host || !opts.getRendition()) {
      return false
    }
    const rect = resolveLocatorRect(locator)
    if (rect === null) {
      return false
    }
    const containerRect = host.getBoundingClientRect()
    const panelTop = containerRect.bottom - obstructionPx
    return rect.bottom < panelTop && rect.bottom > containerRect.top
  }

  function readingPanelAnchorTopPx(
    locator: ContentLocatorFull,
    obstructionPx: number
  ): number | null {
    if (!isLocatorBottomVisible(locator, obstructionPx)) {
      return null
    }
    const host = opts.hostRef.value
    if (!host || !opts.getRendition()) {
      return null
    }
    const rect = resolveLocatorRect(locator)
    if (rect === null) {
      return null
    }
    const containerRect = host.getBoundingClientRect()
    return rect.bottom - containerRect.top + READING_PANEL_ANCHOR_GAP_PX
  }

  return {
    viewBlockStarts,
    resolveLocatorRect,
    isLocatorBottomVisible,
    readingPanelAnchorTopPx,
  }
}
