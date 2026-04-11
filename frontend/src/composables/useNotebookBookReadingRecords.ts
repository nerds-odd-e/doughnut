import {
  readingDispositionByBlockId,
  type BookBlockReadingDisposition,
} from "@/lib/book-reading/readBlockIdsFromRecords"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { BookBlockReadingRecordListItem } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, ref, toValue, type MaybeRefOrGetter } from "vue"

export function useNotebookBookReadingRecords(
  notebookId: MaybeRefOrGetter<number>
) {
  const rows = ref<BookBlockReadingRecordListItem[]>([])

  const dispositionByBlockId = computed(() =>
    readingDispositionByBlockId(rows.value)
  )

  async function syncFromServer(): Promise<void> {
    const res = await NotebookBooksController.getNotebookBookReadingRecords({
      path: { notebook: toValue(notebookId) },
    })
    if (!res.error && res.data) {
      rows.value = res.data
    }
  }

  async function submitReadingDisposition(
    bookBlockId: number,
    status: BookBlockReadingDisposition
  ): Promise<boolean> {
    const result = await apiCallWithLoading(() =>
      NotebookBooksController.putNotebookBookBlockReadingRecord({
        path: {
          notebook: toValue(notebookId),
          bookBlock: bookBlockId,
        },
        body: { status },
      })
    )
    if (result.error || result.data === undefined) {
      return false
    }
    rows.value = result.data
    return true
  }

  function hasRecordedDisposition(blockId: number): boolean {
    return dispositionByBlockId.value.has(blockId)
  }

  function dispositionForBlock(
    blockId: number
  ): BookBlockReadingDisposition | undefined {
    return dispositionByBlockId.value.get(blockId)
  }

  return {
    syncFromServer,
    submitReadingDisposition,
    hasRecordedDisposition,
    dispositionForBlock,
  }
}
