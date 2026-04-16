<template>
  <GlobalBar>
    <BookLayoutToggleButton
      v-model:opened="bookLayoutOpened"
      :panel-id="bookReadingBookLayoutPanelId"
    />
    <router-link
      :to="{ name: 'notebookEdit', params: { notebookId: notebookId } }"
      class="daisy-btn daisy-btn-sm daisy-btn-ghost daisy-shrink-0 daisy-no-underline"
    >
      Notebook
    </router-link>
    <span
      class="daisy-truncate daisy-text-sm daisy-font-medium daisy-min-w-0 daisy-ml-1"
      data-testid="book-reading-epub-global-bar-title"
      :title="book.bookName"
    >
      {{ book.bookName }}
    </span>
    <span class="daisy-ml-auto daisy-shrink-0" aria-hidden="true" />
  </GlobalBar>
  <BookReadingBookLayout
    v-model:opened="bookLayoutOpened"
    :panel-id="bookReadingBookLayoutPanelId"
    :is-md-or-larger="isMdOrLarger"
    :blocks="book.blocks"
    :current-block-id="null"
    :selected-block-id="null"
    :disposition-for-block="noDisposition"
    @block-click="onBookBlockClick"
  >
    <main
      class="daisy-flex daisy-flex-1 daisy-min-h-0 daisy-min-w-0 daisy-flex-col"
    >
      <EpubBookViewer
        ref="epubViewerRef"
        :epub-bytes="epubBytes"
        :book="book"
      />
    </main>
  </BookReadingBookLayout>
</template>

<script setup lang="ts">
import BookLayoutToggleButton from "@/components/book-reading/BookLayoutToggleButton.vue"
import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import EpubBookViewer from "@/components/book-reading/EpubBookViewer.vue"
import GlobalBar from "@/components/toolbars/GlobalBar.vue"
import type { BookBlockFull, BookFull } from "@generated/doughnut-backend-api"
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from "vue"

type EpubViewerExposed = {
  displaySpineHref: (href: string) => Promise<void>
  getRenditionHost?: () => HTMLElement | null
}

const BOOK_READING_LAYOUT_BREAKPOINT_PX = 768
const bookReadingBookLayoutPanelId = "book-reading-book-layout-panel"

const props = defineProps<{
  book: BookFull
  epubBytes: ArrayBuffer
}>()

const notebookId = computed(() => Number(props.book.notebookId))

const epubViewerRef = ref<EpubViewerExposed | null>(null)

const bookLayoutOpened = ref(false)
const windowWidth = ref(
  typeof window !== "undefined"
    ? window.innerWidth
    : BOOK_READING_LAYOUT_BREAKPOINT_PX
)

function handleResize() {
  windowWidth.value = window.innerWidth
}

const isMdOrLarger = computed(
  () => windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX
)

function noDisposition(_blockId: number) {
  return undefined
}

function elementWithIdInIframeDoc(
  doc: Document,
  id: string
): HTMLElement | null {
  const q = doc.querySelector(`[id="${CSS.escape(id)}"]`)
  if (q instanceof HTMLElement) {
    return q
  }
  const body = doc.body
  if (!body) {
    return null
  }
  const all = body.getElementsByTagName("*")
  for (let i = 0; i < all.length; i++) {
    const e = all[i]!
    if (e.getAttribute("id") === id) {
      return e as HTMLElement
    }
  }
  return null
}

function scrollReadingAreaToRevealElement(
  iframe: HTMLIFrameElement,
  el: HTMLElement,
  padding: number
) {
  el.scrollIntoView({ block: "start", inline: "nearest", behavior: "auto" })

  const iframeWin = iframe.contentWindow
  if (iframeWin && iframe.scrollHeight > iframe.clientHeight + 1) {
    const rect = el.getBoundingClientRect()
    const top = rect.top + iframeWin.scrollY - padding
    iframeWin.scrollTo({ top: Math.max(0, top), behavior: "auto" })
  }

  const verticalScrollers: HTMLElement[] = []
  let node: HTMLElement | null = iframe
  while (node) {
    const { overflowY } = window.getComputedStyle(node)
    const canScrollY =
      (overflowY === "auto" ||
        overflowY === "scroll" ||
        overflowY === "overlay") &&
      node.scrollHeight > node.clientHeight + 1
    if (canScrollY) {
      verticalScrollers.push(node)
    }
    node = node.parentElement
  }
  for (const sc of verticalScrollers) {
    const elRect = el.getBoundingClientRect()
    const sr = sc.getBoundingClientRect()
    sc.scrollTop += elRect.top - sr.top - padding
  }
  if (verticalScrollers.length === 0) {
    const elRect = el.getBoundingClientRect()
    window.scrollBy({ top: elRect.top - 120 - padding, behavior: "auto" })
  }

  const mainContent = document.querySelector(".main-content")
  if (mainContent instanceof HTMLElement) {
    const elRect = el.getBoundingClientRect()
    const mr = mainContent.getBoundingClientRect()
    mainContent.scrollTop += elRect.top - mr.top - padding
  }
}

async function scrollEpubRenditionHostToFragmentId(
  host: HTMLElement,
  fragmentId: string
): Promise<void> {
  const padding = 16
  const settle = () =>
    new Promise<void>((resolve) => {
      requestAnimationFrame(() => requestAnimationFrame(() => resolve()))
    })

  for (let attempt = 0; attempt < 40; attempt++) {
    await settle()
    for (const iframe of host.querySelectorAll("iframe")) {
      const doc = iframe.contentDocument
      if (!doc) {
        continue
      }
      const el = elementWithIdInIframeDoc(doc, fragmentId)
      if (el instanceof HTMLElement) {
        scrollReadingAreaToRevealElement(iframe, el, padding)
        return
      }
    }
    await new Promise((r) => setTimeout(r, 40))
  }
}

async function onBookBlockClick(block: BookBlockFull) {
  const href = block.epubStartHref
  if (!href) {
    return
  }
  const viewer = epubViewerRef.value
  if (!viewer) {
    return
  }
  await viewer.displaySpineHref(href)
  await nextTick()
  await new Promise<void>((r) => setTimeout(r, 150))
  const hash = href.indexOf("#")
  if (hash < 0 || hash >= href.length - 1) {
    return
  }
  const fragmentId = href.slice(hash + 1).trim()
  if (!fragmentId) {
    return
  }
  const host =
    viewer.getRenditionHost?.() ??
    document.querySelector<HTMLElement>(
      '[data-testid="epub-book-viewer"] .epub-book-viewer-host'
    )
  if (host) {
    await scrollEpubRenditionHostToFragmentId(host, fragmentId)
  }
}

onMounted(() => {
  window.addEventListener("resize", handleResize)
  if (windowWidth.value >= BOOK_READING_LAYOUT_BREAKPOINT_PX) {
    bookLayoutOpened.value = true
  }
})

onBeforeUnmount(() => {
  window.removeEventListener("resize", handleResize)
})
</script>
