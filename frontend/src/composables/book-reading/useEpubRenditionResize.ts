import { epubRenditionResizeDimensions } from "@/lib/book-reading/epubRenditionHostSize"
import type { Rendition } from "epubjs"
import type { Ref } from "vue"

const RENDITION_RESIZE_DEBOUNCE_MS = 100
const RENDITION_RESIZE_MIN_DELTA_PX = 2

function renditionHasLocation(r: Rendition): boolean {
  const loc = (r as unknown as { location?: { start?: unknown } }).location
  return Boolean(loc?.start)
}

/**
 * epub.js resize clears all views; `onResized` only redisplays when `rendition.location`
 * is set, but `reportLocation` runs after the display promise resolves (queue + rAF).
 * Defer host-driven resize until a location exists so we never clear without a redisplay.
 */
function waitUntilRenditionLocation(r: Rendition): Promise<void> {
  if (renditionHasLocation(r)) {
    return Promise.resolve()
  }
  return new Promise((resolve) => {
    let done = false
    const finish = () => {
      if (done) {
        return
      }
      done = true
      r.off("relocated", onRelocated)
      window.clearTimeout(tid)
      resolve()
    }
    const onRelocated = () => {
      if (renditionHasLocation(r)) {
        finish()
      }
    }
    r.on("relocated", onRelocated)
    const tid = window.setTimeout(finish, 500)
  })
}

/** Keeps the epub.js rendition sized to its host element. */
export function useEpubRenditionResize(opts: {
  hostRef: Ref<HTMLElement | null>
  getRendition: () => Rendition | null
}) {
  let observer: ResizeObserver | null = null
  let debounceTimer: ReturnType<typeof setTimeout> | null = null
  let lastAppliedWidth = 0
  let lastAppliedHeight = 0

  function resizeRenditionToHost(): void {
    const host = opts.hostRef.value
    const r = opts.getRendition()
    if (!host || !r) {
      return
    }
    const dims = epubRenditionResizeDimensions(host)
    if (dims === null) {
      return
    }
    const { width: w, height: h } = dims
    if (
      Math.abs(w - lastAppliedWidth) < RENDITION_RESIZE_MIN_DELTA_PX &&
      Math.abs(h - lastAppliedHeight) < RENDITION_RESIZE_MIN_DELTA_PX
    ) {
      return
    }
    lastAppliedWidth = w
    lastAppliedHeight = h
    r.resize(w, h)
  }

  function stopObservingHostResize(): void {
    if (debounceTimer !== null) {
      clearTimeout(debounceTimer)
      debounceTimer = null
    }
    observer?.disconnect()
    observer = null
    lastAppliedWidth = 0
    lastAppliedHeight = 0
  }

  async function observeHostResize(r: Rendition): Promise<void> {
    await waitUntilRenditionLocation(r)
    stopObservingHostResize()
    const host = opts.hostRef.value
    if (!host || typeof ResizeObserver === "undefined") {
      return
    }
    // Prime from current host size so we only call r.resize() when the host actually changes.
    // epub.js was initialized with width/height = "100%" of this same host.
    const primed = epubRenditionResizeDimensions(host)
    if (primed !== null) {
      lastAppliedWidth = primed.width
      lastAppliedHeight = primed.height
    }
    observer = new ResizeObserver(() => {
      if (debounceTimer !== null) {
        clearTimeout(debounceTimer)
      }
      debounceTimer = setTimeout(() => {
        debounceTimer = null
        resizeRenditionToHost()
      }, RENDITION_RESIZE_DEBOUNCE_MS)
    })
    observer.observe(host)
  }

  return { observeHostResize, stopObservingHostResize }
}
