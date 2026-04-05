<template>
  <canvas ref="canvasRef" data-testid="pdf-first-page-canvas" />
</template>

<script setup lang="ts">
import "@/lib/pdfjsWorker"
import { getDocument } from "pdfjs-dist"
import { nextTick, ref, watch } from "vue"

const props = defineProps<{
  pdfBytes: ArrayBuffer | Uint8Array | null
}>()

const canvasRef = ref<HTMLCanvasElement | null>(null)
let renderSeq = 0

async function renderPage() {
  const bytes = props.pdfBytes
  const canvas = canvasRef.value
  if (!bytes?.byteLength || !canvas) return

  const seq = ++renderSeq
  const data =
    bytes instanceof Uint8Array ? new Uint8Array(bytes) : new Uint8Array(bytes)

  const loadingTask = getDocument({ data })
  try {
    const pdf = await loadingTask.promise
    if (seq !== renderSeq) return

    const page = await pdf.getPage(1)
    if (seq !== renderSeq) return

    const viewport = page.getViewport({ scale: 1 })
    const ctx = canvas.getContext("2d")
    if (!ctx) return

    canvas.width = viewport.width
    canvas.height = viewport.height

    await page.render({ canvasContext: ctx, viewport }).promise
  } finally {
    await loadingTask.destroy()
  }
}

watch(
  () => props.pdfBytes,
  async () => {
    await nextTick()
    await renderPage()
  },
  { immediate: true }
)
</script>
