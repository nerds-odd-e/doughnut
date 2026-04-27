<template>
  <div
    data-testid="epub-book-viewer"
    class="epub-book-viewer-root daisy-relative daisy-min-h-0 daisy-min-w-0 daisy-flex-1"
    :aria-label="book.bookName"
  >
    <div
      ref="renditionHostRef"
      class="epub-book-viewer-host daisy-absolute daisy-inset-0 daisy-overflow-hidden"
    />
  </div>
</template>

<script setup lang="ts">
import type { ViewerLocatorRect } from "@/composables/bookReaderViewerRef"
import { asEpubLocator } from "@/lib/book-reading/asEpubLocator"
import {
  epubSpinePathMatches,
  resolveSpineHrefForStoredPath,
  splitEpubHref,
} from "@/lib/book-reading/epubHrefMatch"
import { epubRenditionResizeDimensions } from "@/lib/book-reading/epubRenditionHostSize"
import type {
  BookFull,
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/doughnut-backend-api"
import ePub, { type Book as EpubJsBook, type Rendition } from "epubjs"
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const RENDITION_RESIZE_DEBOUNCE_MS = 100
const RENDITION_RESIZE_MIN_DELTA_PX = 2

const READING_PANEL_ANCHOR_GAP_PX = 8

type EpubRenditionIframeView = {
  displayed?: boolean
  section?: { href?: string }
  contents?: { document?: Document }
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

const props = withDefaults(
  defineProps<{
    epubBytes: ArrayBuffer
    book: BookFull
    initialLocator?: string | null
  }>(),
  { initialLocator: null }
)

const emit = defineEmits<{
  relocated: [payload: { href: string }]
}>()

const renditionHostRef = ref<HTMLElement | null>(null)
let bookInstance: EpubJsBook | null = null
let rendition: Rendition | null = null
let renditionResizeObserver: ResizeObserver | null = null
let renditionResizeDebounceTimer: ReturnType<typeof setTimeout> | null = null
let lastAppliedRenditionWidth = 0
let lastAppliedRenditionHeight = 0

function resizeRenditionToHost(): void {
  const host = renditionHostRef.value
  const r = rendition
  if (!host || !r) {
    return
  }
  const dims = epubRenditionResizeDimensions(host)
  if (dims === null) {
    return
  }
  const { width: w, height: h } = dims
  if (
    Math.abs(w - lastAppliedRenditionWidth) < RENDITION_RESIZE_MIN_DELTA_PX &&
    Math.abs(h - lastAppliedRenditionHeight) < RENDITION_RESIZE_MIN_DELTA_PX
  ) {
    return
  }
  lastAppliedRenditionWidth = w
  lastAppliedRenditionHeight = h
  r.resize(w, h)
}

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

function teardownRenditionResizeObserver(): void {
  if (renditionResizeDebounceTimer !== null) {
    clearTimeout(renditionResizeDebounceTimer)
    renditionResizeDebounceTimer = null
  }
  renditionResizeObserver?.disconnect()
  renditionResizeObserver = null
  lastAppliedRenditionWidth = 0
  lastAppliedRenditionHeight = 0
}

function setupRenditionResizeObserver(): void {
  teardownRenditionResizeObserver()
  const host = renditionHostRef.value
  if (!host || typeof ResizeObserver === "undefined") {
    return
  }
  // Prime from current host size so we only call r.resize() when the host actually changes.
  // epub.js was initialized with width/height = "100%" of this same host.
  const primed = epubRenditionResizeDimensions(host)
  if (primed !== null) {
    lastAppliedRenditionWidth = primed.width
    lastAppliedRenditionHeight = primed.height
  }
  renditionResizeObserver = new ResizeObserver(() => {
    if (renditionResizeDebounceTimer !== null) {
      clearTimeout(renditionResizeDebounceTimer)
    }
    renditionResizeDebounceTimer = setTimeout(() => {
      renditionResizeDebounceTimer = null
      resizeRenditionToHost()
    }, RENDITION_RESIZE_DEBOUNCE_MS)
  })
  renditionResizeObserver.observe(host)
}

function emitIfHref(href: string | undefined) {
  if (typeof href === "string" && href.length > 0) {
    emit("relocated", { href })
  }
}

/**
 * epub.js's `relocated` does not always fire on the initial `display()` in continuous/scrolled
 * mode, so we also listen to `displayed` (fires when a section first mounts) to guarantee the
 * initial current block is reported. Both deliver the spine href we need.
 */
/**
 * In continuous/scrolled mode, `start` is the topmost visible section and `end` the bottommost.
 * Using `end` (when set) matches reading position when more than one spine item intersects
 * the viewport (e.g. a tall window shows the tail of ch.N and the start of ch.N+1).
 */
const onRelocated = (location: {
  start?: { href?: string }
  end?: { href?: string }
}) => {
  const href = location.end?.href ?? location.start?.href
  emitIfHref(href)
}
const onDisplayed = (section: { href?: string }) => emitIfHref(section.href)

type EpubSpineItem = { href?: string }

function spineItems(b: EpubJsBook | null): ReadonlyArray<EpubSpineItem> {
  const raw = (
    b as unknown as { spine?: { spineItems?: ReadonlyArray<EpubSpineItem> } }
  )?.spine?.spineItems
  return Array.isArray(raw) ? raw : []
}

/**
 * Resolve a stored locator to an epub.js display target. The backend stores package-root
 * paths (e.g. `OEBPS/chapter3.xhtml`) while epub.js indexes sections by the raw manifest
 * href (e.g. `chapter3.xhtml`), so we must translate before calling `rendition.display`.
 */
function epubDisplayTarget(epub: EpubLocatorFull): string | null {
  const storedPath = splitEpubHref(epub.href.trim()).path
  if (storedPath.length === 0) {
    return null
  }
  const spineHref =
    resolveSpineHrefForStoredPath(spineItems(bookInstance), storedPath) ??
    storedPath
  const frag = epub.fragment?.trim() ?? ""
  return frag.length === 0 ? spineHref : `${spineHref}#${frag}`
}

async function displayLocator(loc: ContentLocatorFull): Promise<void> {
  const epub = asEpubLocator(loc)
  if (!epub || !rendition) {
    return
  }
  const target = epubDisplayTarget(epub)
  if (!target) {
    return
  }
  const r = rendition
  await r.display(target).catch(() => undefined)
  await nextTick()
  await new Promise<void>((r0) => setTimeout(r0, 100))
  let el: HTMLElement | null = resolveEpubLocatorElement(r, epub)
  if (el && /^H[1-6]$/i.test(el.tagName)) {
    const next = el.nextElementSibling
    if (
      next instanceof HTMLElement &&
      (next.textContent?.trim().length ?? 0) > 0
    ) {
      el = next
    }
  }
  if (el) {
    el.scrollIntoView({ block: "center", inline: "nearest" })
  }
}

function resolveLocatorRect(
  locator: ContentLocatorFull
): ViewerLocatorRect | null {
  const host = renditionHostRef.value
  const r = rendition
  if (!host || !r) {
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
  const host = renditionHostRef.value
  if (!host || !rendition) {
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
  const host = renditionHostRef.value
  if (!host || !rendition) {
    return null
  }
  const rect = resolveLocatorRect(locator)
  if (rect === null) {
    return null
  }
  const containerRect = host.getBoundingClientRect()
  return rect.bottom - containerRect.top + READING_PANEL_ANCHOR_GAP_PX
}

defineExpose({
  displayLocator,
  resolveLocatorRect,
  isLocatorBottomVisible,
  readingPanelAnchorTopPx,
})

function destroyEpub() {
  teardownRenditionResizeObserver()
  if (rendition) {
    rendition.off("relocated", onRelocated)
    rendition.off("displayed", onDisplayed)
  }
  rendition?.destroy()
  rendition = null
  bookInstance?.destroy()
  bookInstance = null
}

async function openEpub() {
  destroyEpub()
  await nextTick()
  const host = renditionHostRef.value
  if (!host || props.epubBytes.byteLength === 0) {
    return
  }

  const b = ePub(props.epubBytes.slice(0), {
    replacements: "blobUrl",
  })
  bookInstance = b
  await b.ready
  const r = b.renderTo(host, {
    flow: "scrolled",
    manager: "continuous",
    width: "100%",
    height: "100%",
    allowScriptedContent: false,
  })
  rendition = r
  r.on("relocated", onRelocated)
  r.on("displayed", onDisplayed)
  const rawInitial = (props.initialLocator ?? "").trim()
  if (rawInitial.length > 0) {
    const { path, fragment } = splitEpubHref(rawInitial)
    const spineHref = resolveSpineHrefForStoredPath(spineItems(b), path) ?? path
    const target =
      fragment !== null && fragment.length > 0
        ? `${spineHref}#${fragment}`
        : spineHref
    await r.display(target).catch(() => r.display().catch(() => undefined))
  } else {
    await r.display().catch(() => undefined)
  }
  await waitUntilRenditionLocation(r)
  setupRenditionResizeObserver()
}

onMounted(() => {
  openEpub().catch(() => undefined)
})

watch(
  () => props.epubBytes,
  () => {
    openEpub().catch(() => undefined)
  }
)

onBeforeUnmount(() => {
  destroyEpub()
})
</script>

<style scoped>
.epub-book-viewer-root {
  position: relative;
  min-height: 0;
}

.epub-book-viewer-host :deep(iframe) {
  border: 0;
}
</style>
