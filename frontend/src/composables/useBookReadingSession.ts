import type { BookReaderViewerRef } from "@/composables/bookReaderViewerRef"
import { useBookReadingCurrentBlock } from "@/composables/useBookReadingCurrentBlock"
import { useBookReadingSelection } from "@/composables/useBookReadingSelection"
import { useNotebookBookReadingRecords } from "@/composables/useNotebookBookReadingRecords"
import { useReadingPanelAnchor } from "@/composables/useReadingPanelAnchor"
import { structuralTitleForBlockId } from "@/lib/book-reading/currentBlockLiveAnnouncement"
import type {
  BookBlockFull,
  BookFull,
  ContentLocatorFull,
} from "@generated/donut-backend-api"
import {
  computed,
  nextTick,
  onMounted,
  ref,
  toValue,
  watch,
  type MaybeRefOrGetter,
  type Ref,
} from "vue"

/** What a format view (PDF, EPUB) offers the reading session. Hooks are called lazily. */
export type BookReadingSurface = {
  /** Land on a chosen block. */
  showBlock: (block: BookBlockFull) => Promise<void>
  /** The locator saved as the last-read position, or null when there is none yet. */
  readingPositionLocator: () => ContentLocatorFull | null
  /** Geometry used to anchor the Reading Control Panel. */
  viewer: Ref<Pick<BookReaderViewerRef, "readingPanelAnchorTopPx"> | null>
  /** The pane the Reading Control Panel is positioned in. */
  mainPane: Ref<HTMLElement | null>
  /** Returns false to keep the current block unchanged (PDF snap-back). */
  commitCurrentBlock?: (id: number | null) => boolean
  /** PDF snap-back decides which block awaits confirmation. */
  blockAwaitingConfirmation?: () => BookBlockFull | null
  /** PDF anchors the panel only while the block's last content is visible. */
  canAnchorPanel?: () => boolean
  onMarkedRead?: (blockId: number) => void
  /** PDF keeps the selection valid when the blocks change. */
  repairSelection?: boolean
  /** EPUB re-anchors the panel after the records sync and after showing a block. */
  reanchorPanelAfterSyncAndShow?: boolean
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

  const {
    blockAwaitingConfirmation,
    applyBookBlockSelection,
    markSelectedBlockDisposition,
  } = useBookReadingSelection({
    bookBlocks,
    currentBlockId,
    hasRecordedDisposition: bookReading.hasRecordedDisposition,
    submitReadingDisposition: bookReading.submitReadingDisposition,
    selectedBlockId,
    repairSelectionWhenBlocksChange: surface.repairSelection,
    overrideBlockAwaitingConfirmation: surface.blockAwaitingConfirmation
      ? computed(surface.blockAwaitingConfirmation)
      : undefined,
    onMarkedRead: surface.onMarkedRead,
    onAdvance: async (block) => {
      await surface.showBlock(block)
      if (surface.reanchorPanelAfterSyncAndShow) await reanchorPanel()
    },
  })

  const { readingPanelAnchorTopPx, updateReadingPanelAnchor } =
    useReadingPanelAnchor({
      viewerRef: surface.viewer,
      blockRef: computed(() =>
        (surface.canAnchorPanel?.() ?? true)
          ? blockAwaitingConfirmation.value
          : null
      ),
      mainPaneRef: surface.mainPane,
    })

  async function reanchorPanel() {
    await nextTick()
    updateReadingPanelAnchor()
  }

  watch(selectedBlockId, () => {
    readingPanelAnchorTopPx.value = null
  })

  const currentBlockLiveText = computed(() =>
    structuralTitleForBlockId(currentBlockId.value, bookBlocks.value)
  )

  onMounted(async () => {
    await bookReading.syncFromServer()
    if (surface.reanchorPanelAfterSyncAndShow) await reanchorPanel()
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
    blockAwaitingConfirmation,
    applyBookBlockSelection,
    markSelectedBlockDisposition,
    readingPanelAnchorTopPx,
    updateReadingPanelAnchor,
  }
}
