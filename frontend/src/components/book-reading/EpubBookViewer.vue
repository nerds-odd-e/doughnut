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
import {
  asEpubLocator,
  epubDisplayHref,
} from "@/lib/book-reading/asEpubLocator"
import {
  epubSpinePathMatches,
  splitEpubHref,
} from "@/lib/book-reading/epubHrefMatch"
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
): Element | null {
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
  const w = Math.floor(host.clientWidth)
  const h = Math.floor(host.clientHeight)
  if (w < 1 || h < 1) {
    return
  }
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
const onRelocated = (location: { start?: { href?: string } }) =>
  emitIfHref(location.start?.href)
const onDisplayed = (section: { href?: string }) => emitIfHref(section.href)

async function displayLocator(loc: ContentLocatorFull): Promise<void> {
  const epub = asEpubLocator(loc)
  if (!epub || !rendition) {
    return
  }
  const h = epubDisplayHref(epub)
  if (!h) {
    return
  }
  await rendition.display(h).catch(() => undefined)
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
  setupRenditionResizeObserver()
  const target = (props.initialLocator ?? "").trim()
  if (target.length > 0) {
    await r.display(target).catch(() => r.display().catch(() => undefined))
  } else {
    await r.display().catch(() => undefined)
  }
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
