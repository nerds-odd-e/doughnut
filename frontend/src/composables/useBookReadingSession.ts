import { useBookReadingCurrentBlock } from "@/composables/useBookReadingCurrentBlock"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import { structuralTitleForBlockId } from "@/lib/book-reading/currentBlockLiveAnnouncement"
import type { BookFull, ContentLocatorFull } from "@generated/donut-backend-api"
import { computed, onMounted, ref, toValue, type MaybeRefOrGetter } from "vue"

/** What a format view (PDF, EPUB) offers the reading session. Hooks are called lazily. */
export type BookReadingSurface = {
  /** The locator saved as the last-read position, or null when there is none yet. */
  readingPositionLocator: () => ContentLocatorFull | null
  /** Returns false to keep the current block unchanged (PDF snap-back). */
  commitCurrentBlock?: (id: number | null) => boolean
  onRecordsSynced?: () => Promise<void>
  /** EPUB sends a pending reading position on leave; PDF drops it. */
  flushPositionOnLeave?: boolean
}

export function useBookReadingSession(options: {
  book: MaybeRefOrGetter<BookFull>
  initialSelectedBlockId: number | null
  surface: BookReadingSurface
}) {
  const { surface } = options
  const notebookId = computed(() => Number(toValue(options.book).notebookId))
  const bookBlocks = computed(() => toValue(options.book).blocks)
  const bookReading = useNotebookBookReadingRecords(notebookId)
  const selectedBlockId = ref<number | null>(options.initialSelectedBlockId)

  const { currentBlockId, currentBlockIdDebouncer, proposeReadingPosition } =
    useBookReadingCurrentBlock({
      notebookId,
      commitCurrentBlock: (id) => surface.commitCurrentBlock?.(id) ?? true,
      flushLastReadPositionPatchOnUnmount: surface.flushPositionOnLeave,
      proposeReadingPosition: (debouncer) => () => {
        const locator = surface.readingPositionLocator()
        if (locator === null) return
        const sel = selectedBlockId.value
        debouncer.propose(locator, sel === null ? undefined : sel)
      },
    })

  const currentBlockLiveText = computed(() =>
    structuralTitleForBlockId(currentBlockId.value, bookBlocks.value)
  )

  onMounted(async () => {
    await bookReading.syncFromServer()
    await surface.onRecordsSynced?.()
  })

  return {
    notebookId,
    bookBlocks,
    bookReading,
    selectedBlockId,
    currentBlockId,
    currentBlockIdDebouncer,
    proposeReadingPosition,
    currentBlockLiveText,
  }
}
