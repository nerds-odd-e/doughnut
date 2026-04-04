<template>
  <div data-testid="book-reading-page">
    <div v-if="book" data-testid="book-reading-outline">
      <div
        v-for="node in flatOutline"
        :key="node.id"
        data-testid="book-outline-node"
        :data-outline-depth="node.depth"
      >
        {{ node.title }}
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from "vue"
import type { BookFull, BookRangeFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"

const props = defineProps({
  notebookId: { type: Number, required: true },
})

const book = ref<BookFull | null>(null)

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
  }
})
</script>
