import { readRangeIdsFromRecords } from "@/lib/book-reading/readRangeIdsFromRecords"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { BookRangeReadingRecordListItem } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { ref, toValue, type MaybeRefOrGetter } from "vue"

export function useNotebookBookReadingRecords(
  notebookId: MaybeRefOrGetter<number>
) {
  const rows = ref<BookRangeReadingRecordListItem[]>([])

  async function syncFromServer(): Promise<void> {
    const res = await NotebookBooksController.getNotebookBookReadingRecords({
      path: { notebook: toValue(notebookId) },
    })
    if (!res.error && res.data) {
      rows.value = res.data
    }
  }

  async function submitMarkRead(bookRangeId: number): Promise<boolean> {
    const result = await apiCallWithLoading(() =>
      NotebookBooksController.putNotebookBookRangeReadingRecord({
        path: {
          notebook: toValue(notebookId),
          bookRange: bookRangeId,
        },
      })
    )
    if (result.error || result.data === undefined) {
      return false
    }
    rows.value = result.data
    return true
  }

  function isDirectContentRead(rangeId: number): boolean {
    return readRangeIdsFromRecords(rows.value).has(rangeId)
  }

  return {
    syncFromServer,
    submitMarkRead,
    isDirectContentRead,
  }
}
