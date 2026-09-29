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
/**
 * epub.js runs one display at a time: a second `display()` resolves the first early and
 * both then drive the view manager, so landing waits for the book's opening display.
 */
let opened: Promise<void> = Promise.resolve()

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

defineExpose({
  displayLocator,
  viewBlockStarts,
  resolveLocatorRect,
  isLocatorBottomVisible,
  readingPanelAnchorTopPx,
})

function destroyEpub() {
  stopObservingHostResize()
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
    const spineHref =
      resolveSpineHrefForStoredPath(epubSpineItems(b), path) ?? path
    const target =
      fragment !== null && fragment.length > 0
        ? `${spineHref}#${fragment}`
        : spineHref
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
