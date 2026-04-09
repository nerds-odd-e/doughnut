import { readBlockIdsFromRecords } from "@/lib/book-reading/readBlockIdsFromRecords"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { BookBlockReadingRecordListItem } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { ref, toValue, type MaybeRefOrGetter } from "vue"

export function useNotebookBookReadingRecords(
  notebookId: MaybeRefOrGetter<number>
) {
  const rows = ref<BookBlockReadingRecordListItem[]>([])

  async function syncFromServer(): Promise<void> {
    const res = await NotebookBooksController.getNotebookBookReadingRecords({
      path: { notebook: toValue(notebookId) },
    })
    if (!res.error && res.data) {
      rows.value = res.data
    }
  }

  async function submitMarkRead(bookBlockId: number): Promise<boolean> {
    const result = await apiCallWithLoading(() =>
      NotebookBooksController.putNotebookBookBlockReadingRecord({
        path: {
          notebook: toValue(notebookId),
          bookBlock: bookBlockId,
        },
      })
    )
    if (result.error || result.data === undefined) {
      return false
    }
    rows.value = result.data
    return true
  }

  function isDirectContentRead(blockId: number): boolean {
    return readBlockIdsFromRecords(rows.value).has(blockId)
  }

  return {
    syncFromServer,
    submitMarkRead,
    isDirectContentRead,
  }
}
