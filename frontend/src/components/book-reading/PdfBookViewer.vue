<template>
  <div
    ref="containerRef"
    data-testid="pdf-book-viewer"
    class="pdf-book-viewer-container"
  >
    <div ref="viewerRef" class="pdfViewer" />
  </div>
</template>

<script setup lang="ts">
import "@/lib/pdfjsWorker"
import { getDocument, type PDFDocumentProxy } from "pdfjs-dist"
import {
  EventBus,
  PDFLinkService,
  PDFViewer,
} from "pdfjs-dist/web/pdf_viewer.mjs"
import "pdfjs-dist/web/pdf_viewer.css"
import { nextTick, onBeforeUnmount, ref, watch } from "vue"

const props = defineProps<{
  pdfBytes: ArrayBuffer | Uint8Array | null
}>()

const emit = defineEmits<{
  loadError: [message: string]
}>()

const containerRef = ref<HTMLDivElement | null>(null)
const viewerRef = ref<HTMLDivElement | null>(null)

let pdfViewer: PDFViewer | null = null
let currentLoadingTask: ReturnType<typeof getDocument> | null = null

async function loadPdf(bytes: ArrayBuffer | Uint8Array) {
  const container = containerRef.value
  const viewer = viewerRef.value
  if (!container || !viewer) return

  if (currentLoadingTask) {
    await currentLoadingTask.destroy()
    currentLoadingTask = null
  }
  if (pdfViewer) {
    pdfViewer.setDocument(null as unknown as PDFDocumentProxy)
    pdfViewer = null
  }

  const eventBus = new EventBus()
  const linkService = new PDFLinkService({ eventBus })

  pdfViewer = new PDFViewer({
    container,
    viewer,
    eventBus,
    linkService,
    removePageBorders: false,
  })
  linkService.setViewer(pdfViewer)

  eventBus.on("pagesinit", () => {
    if (pdfViewer) {
      const containerWidth = container.clientWidth
      pdfViewer.currentScaleValue = containerWidth > 0 ? "page-width" : "1"
    }
  })

  const data =
    bytes instanceof Uint8Array ? new Uint8Array(bytes) : new Uint8Array(bytes)
  const loadingTask = getDocument({ data })
  currentLoadingTask = loadingTask

  try {
    const pdf = await loadingTask.promise
    if (currentLoadingTask !== loadingTask) return
    pdfViewer.setDocument(pdf)
    linkService.setDocument(pdf)
  } catch {
    if (currentLoadingTask === loadingTask) {
      emit("loadError", "This file is not a valid PDF.")
    }
  }
}

watch(
  () => props.pdfBytes,
  async (bytes) => {
    await nextTick()
    if (bytes?.byteLength) {
      await loadPdf(bytes)
    }
  },
  { immediate: true }
)

onBeforeUnmount(async () => {
  if (currentLoadingTask) {
    await currentLoadingTask.destroy()
    currentLoadingTask = null
  }
  if (pdfViewer) {
    pdfViewer.setDocument(null as unknown as PDFDocumentProxy)
    pdfViewer = null
  }
})
</script>

<style scoped>
.pdf-book-viewer-container {
  overflow: auto;
  position: absolute;
  inset: 0;
  background-color: #808080;
}
</style>
