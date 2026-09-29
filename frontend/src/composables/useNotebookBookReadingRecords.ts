import {
  readingDispositionByBlockId,
  type BookBlockReadingDisposition,
} from "@/lib/book-reading/readBlockIdsFromRecords"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import type { BookBlockReadingRecordListItem } from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
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

  async function replaceRowsWith(
    call: () => Promise<{
      data?: BookBlockReadingRecordListItem[]
      error?: unknown
    }>
  ): Promise<boolean> {
    const result = await apiCallWithLoading(call)
    if (result.error || result.data === undefined) {
      return false
    }
    rows.value = result.data
    return true
  }

  function submitReadingDisposition(
    bookBlockId: number,
    status: BookBlockReadingDisposition
  ): Promise<boolean> {
    return replaceRowsWith(() =>
      NotebookBooksController.putNotebookBookBlockReadingRecord({
        path: { notebook: toValue(notebookId), bookBlock: bookBlockId },
        body: { status },
      })
    )
  }

  function clearReadingDisposition(bookBlockId: number): Promise<boolean> {
    return replaceRowsWith(() =>
      NotebookBooksController.deleteNotebookBookBlockReadingRecord({
        path: { notebook: toValue(notebookId), bookBlock: bookBlockId },
      })
    )
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
    clearReadingDisposition,
    hasRecordedDisposition,
    dispositionForBlock,
  }
}
