<template>
  <div
    data-testid="epub-book-viewer"
    class="epub-book-viewer-root daisy-relative daisy-min-h-0 daisy-min-w-0 daisy-flex-1"
    :aria-label="book.bookName"
  >
    <div
      ref="renditionHostRef"
      class="epub-book-viewer-host daisy-absolute daisy-inset-0 daisy-overflow-auto"
    />
  </div>
</template>

<script setup lang="ts">
import type { BookFull } from "@generated/doughnut-backend-api"
import ePub, { type Book as EpubJsBook, type Rendition } from "epubjs"
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"

const props = defineProps<{
  epubBytes: ArrayBuffer
  book: BookFull
}>()

const renditionHostRef = ref<HTMLElement | null>(null)
let bookInstance: EpubJsBook | null = null
let rendition: Rendition | null = null

async function displaySpineHref(href: string) {
  const h = href.trim()
  if (!h || !rendition) {
    return
  }
  await rendition.display(h).catch(() => undefined)
}

defineExpose({
  displaySpineHref,
  getRenditionHost: () => renditionHostRef.value,
})

function destroyEpub() {
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
    allowScriptedContent: false,
  })
  rendition = r
  await r.display()
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
