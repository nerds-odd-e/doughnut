<template>
  <div data-testid="book-reading-page">
    <template v-if="book">
      <a
        v-if="book.hasSourceFile"
        class="daisy-btn daisy-btn-outline daisy-btn-sm daisy-mb-2"
        :href="notebookBookFilePath(props.notebookId)"
        data-testid="book-reading-download"
      >
        Download
      </a>
      <ContentLoader v-if="pdfLoading" />
      <div
        v-else-if="pdfError"
        class="daisy-alert daisy-alert-error daisy-mb-2"
        data-testid="book-reading-pdf-error"
      >
        {{ pdfError }}
      </div>
      <PdfFirstPageCanvas
        v-else-if="bookPdfBytes"
        :pdf-bytes="bookPdfBytes"
        @load-error="onPdfLoadError"
      />
      <div data-testid="book-reading-outline">
        <div
          v-for="node in flatOutline"
          :key="node.id"
          data-testid="book-outline-node"
          :data-outline-depth="node.depth"
        >
          {{ node.title }}
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import ContentLoader from "@/components/commons/ContentLoader.vue"
import PdfFirstPageCanvas from "@/components/book-reading/PdfFirstPageCanvas.vue"
import type { BookFull, BookRangeFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { onMounted, ref } from "vue"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

function notebookBookFilePath(notebookId: number) {
  return `/api/notebooks/${notebookId}/book/file`
}

const book = ref<BookFull | null>(null)
const bookPdfBytes = ref<ArrayBuffer | null>(null)
const pdfLoading = ref(false)
const pdfError = ref<string | null>(null)

function onPdfLoadError(message: string) {
  pdfError.value = message
}

type OutlineNode = { id: number; title: string; depth: number }

function buildFlatOutline(ranges: BookRangeFull[]): OutlineNode[] {
  const childrenMap = new Map<number | null, BookRangeFull[]>()
  for (const range of ranges) {
    const parentId =
      range.parentRangeId != null ? Number(range.parentRangeId) : null
    const siblings = childrenMap.get(parentId) ?? []
    siblings.push(range)
    childrenMap.set(parentId, siblings)
  }
  const sortByOrder = (a: BookRangeFull, b: BookRangeFull) =>
    (a.siblingOrder ?? 0) - (b.siblingOrder ?? 0)

  const result: OutlineNode[] = []
  function visit(parentId: number | null, depth: number) {
    const children = (childrenMap.get(parentId) ?? []).slice().sort(sortByOrder)
    for (const child of children) {
      result.push({ id: child.id, title: child.title, depth })
      visit(child.id, depth + 1)
    }
  }
  visit(null, 0)
  return result
}

const flatOutline = ref<OutlineNode[]>([])

onMounted(async () => {
  const { data, error } = await NotebookBooksController.getBook({
    path: { notebook: props.notebookId },
  })
  if (!error && data) {
    book.value = data
    flatOutline.value = buildFlatOutline(data.ranges ?? [])
    if (data.hasSourceFile) {
      pdfLoading.value = true
      pdfError.value = null
      try {
        const res = await fetch(notebookBookFilePath(props.notebookId), {
          credentials: "same-origin",
        })
        if (!res.ok) {
          pdfError.value = "Could not load the book file."
          return
        }
        bookPdfBytes.value = await res.arrayBuffer()
      } catch {
        pdfError.value = "Could not load the book file."
      } finally {
        pdfLoading.value = false
      }
    }
  }
})
</script>
