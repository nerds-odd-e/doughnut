<template>
  <div
    data-testid="epub-book-viewer"
    class="epub-book-viewer-root relative min-h-0 min-w-0 flex-1"
    :aria-label="book.bookName"
  >
    <div
      ref="renditionHostRef"
      class="epub-book-viewer-host absolute inset-0 overflow-hidden"
    />
  </div>
</template>

<script setup lang="ts">
import {
  epubSpineItems,
  useEpubLocatorGeometry,
} from "@/composables/book-reading/useEpubLocatorGeometry"
import { useEpubRenditionResize } from "@/composables/book-reading/useEpubRenditionResize"
import { asEpubLocator } from "@/lib/book-reading/asEpubLocator"
import {
  resolveSpineHrefForStoredPath,
  splitEpubHref,
} from "@/lib/book-reading/epubHrefMatch"
import type {
  BookFull,
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/donut-backend-api"
import ePub, { type Book as EpubJsBook, type Rendition } from "epubjs"
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = withDefaults(
  defineProps<{
    epubBytes: ArrayBuffer
    book: BookFull
    initialLocator?: ContentLocatorFull | null
  }>(),
  { initialLocator: null }
)

const emit = defineEmits<{
  relocated: []
}>()

const renditionHostRef = ref<HTMLElement | null>(null)
let bookInstance: EpubJsBook | null = null
let rendition: Rendition | null = null
/** Resolves once the book is open and its opening display has landed. */
let opened: Promise<void> = Promise.resolve()

/**
 * epub.js runs one display at a time: a second `display()` resolves the first early and
 * clears its views while the first is still filling them, which stalls the view manager for
 * good. Every display (the opening one, a chosen block, a link inside the book, epub.js's own
 * redisplay after a resize) therefore waits for the previous one to land.
 */
function landOneDisplayAtATime(r: Rendition) {
  const display = r.display.bind(r) as (
    target?: string | number
  ) => Promise<void>
  let landed: Promise<unknown> = Promise.resolve()
  r.display = (target?: string | number) => {
    const landing = landed.then(() => display(target))
    landed = landing.catch(() => undefined)
    return landing
  }
}

const { observeHostResize, stopObservingHostResize } = useEpubRenditionResize({
  hostRef: renditionHostRef,
  getRendition: () => rendition,
})

const {
  viewBlockStarts,
  resolveLocatorRect,
  isLocatorBottomVisible,
  readingPanelAnchorTopPx,
} = useEpubLocatorGeometry({
  hostRef: renditionHostRef,
  getRendition: () => rendition,
  getBook: () => bookInstance,
})

/**
 * epub.js's `relocated` does not always fire on the initial `display()` in continuous/scrolled
 * mode, so `displayed` (fires when a section first mounts) also reports that the view moved.
 */
const emitRelocated = () => emit("relocated")

/**
 * Resolve a stored locator to an epub.js display target. A locator with an exact place (CFI)
 * displays there; otherwise at href#fragment. The backend stores package-root paths (e.g.
 * `OEBPS/chapter3.xhtml`) while epub.js indexes sections by the raw manifest href (e.g.
 * `chapter3.xhtml`), so we must translate before calling `rendition.display`.
 */
function epubDisplayTarget(epub: EpubLocatorFull): string | null {
  const cfi = epub.cfi?.trim() ?? ""
  if (cfi.length > 0) {
    return cfi
  }
  const storedPath = splitEpubHref(epub.href.trim()).path
  if (storedPath.length === 0) {
    return null
  }
  const spineHref =
    resolveSpineHrefForStoredPath(epubSpineItems(bookInstance), storedPath) ??
    storedPath
  const frag = epub.fragment?.trim() ?? ""
  return frag.length === 0 ? spineHref : `${spineHref}#${frag}`
}

async function displayLocator(loc: ContentLocatorFull): Promise<void> {
  await opened
  const epub = asEpubLocator(loc)
  if (!epub || !rendition) {
    return
  }
  const target = epubDisplayTarget(epub)
  if (!target) {
    return
  }
  await rendition.display(target).catch(() => undefined)
}

/** The exact place (CFI) at the top of the reader's view, once epub.js has located it. */
function currentCfi(): string | undefined {
  const location = rendition?.currentLocation() as unknown as
    | { start?: { cfi?: string } }
    | undefined
  return location?.start?.cfi
}

defineExpose({
  displayLocator,
  currentCfi,
  viewBlockStarts,
  resolveLocatorRect,
  isLocatorBottomVisible,
  readingPanelAnchorTopPx,
})

function destroyEpub() {
  stopObservingHostResize()
  if (rendition) {
    rendition.off("relocated", emitRelocated)
    rendition.off("displayed", emitRelocated)
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
  landOneDisplayAtATime(r)
  r.on("relocated", emitRelocated)
  r.on("displayed", emitRelocated)
  const initial = asEpubLocator(props.initialLocator ?? undefined)
  const target = initial ? epubDisplayTarget(initial) : null
  if (target) {
    await r.display(target).catch(() => r.display().catch(() => undefined))
  } else {
    await r.display().catch(() => undefined)
  }
  await observeHostResize(r)
}

function open() {
  opened = openEpub().catch(() => undefined)
}

onMounted(open)

watch(() => props.epubBytes, open)

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

/*
 * epub.js corrects scrollTop itself when it prepends earlier sections; browser scroll
 * anchoring would shift by the same height again and land the target far below.
 */
.epub-book-viewer-host :deep(.epub-container) {
  overflow-anchor: none;
}
</style>
