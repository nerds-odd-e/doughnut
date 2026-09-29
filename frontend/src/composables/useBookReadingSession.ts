import type { BookReaderViewerRef } from "@/composables/bookReaderViewerRef"
import {
  bookFullAfterLayoutMutation,
  useBookLayoutMutations,
} from "@/composables/book-reading/useBookLayoutMutations"
import { useBookLayoutAiReorganize } from "@/composables/useBookLayoutAiReorganize"
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
  type ComputedRef,
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
  /** PDF's reading panel target decides which block awaits confirmation. */
  blockAwaitingConfirmation?: () => BookBlockFull | null
  /** PDF anchors the panel to this block, only while its last content is visible. */
  anchoredBlock?: () => BookBlockFull | null
  /** PDF keeps the selection valid when the blocks change. */
  repairSelection?: boolean
  /** EPUB re-anchors the panel after the records sync and after showing a block. */
  reanchorPanelAfterSyncAndShow?: boolean
  /** EPUB sends a pending reading position on leave; PDF drops it. */
  flushPositionOnLeave?: boolean
  /** PDF reorganizes the book layout, receiving the updated book, and shows the "Now reading" bar. */
  reorganize?: { onBookUpdated: (book: BookFull) => void }
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
  /** The pane the Reading Control Panel is positioned in; set by the reading shell. */
  const mainPane = ref<HTMLElement | null>(null)

  const { currentBlockId, currentBlockIdDebouncer, proposeReadingPosition } =
    useBookReadingCurrentBlock({
      notebookId,
      flushPositionOnLeave: surface.flushPositionOnLeave,
      readingPosition: () => {
        const locator = surface.readingPositionLocator()
        if (locator === null) return null
        return {
          locator,
          selectedBookBlockId: selectedBlockId.value ?? undefined,
        }
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
    repairSelection: surface.repairSelection,
    blockAwaitingConfirmation: surface.blockAwaitingConfirmation,
    onAdvance: async (block) => {
      await surface.showBlock(block)
      if (surface.reanchorPanelAfterSyncAndShow) await reanchorPanel()
    },
  })

  const { readingPanelAnchorTopPx, updateReadingPanelAnchor } =
    useReadingPanelAnchor({
      viewerRef: surface.viewer,
      blockRef: computed(() =>
        surface.anchoredBlock
          ? surface.anchoredBlock()
          : blockAwaitingConfirmation.value
      ),
      mainPaneRef: mainPane,
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

  const reorganize = surface.reorganize
    ? useReorganize({
        getBook: () => toValue(options.book),
        notebookId,
        bookBlocks,
        selectedBlockId,
        currentBlockId,
        applyBookBlockSelection,
        onBookUpdated: surface.reorganize.onBookUpdated,
      })
    : null

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
    mainPane,
    reorganize,
  }
}

/** Layout changes, AI reorganize, and the "Now reading" bar's navigation. */
function useReorganize(opts: {
  getBook: () => BookFull
  notebookId: ComputedRef<number>
  bookBlocks: ComputedRef<BookBlockFull[]>
  selectedBlockId: Ref<number | null>
  currentBlockId: Ref<number | null>
  applyBookBlockSelection: (block: BookBlockFull) => Promise<void>
  onBookUpdated: (book: BookFull) => void
}) {
  const { bookBlocks, selectedBlockId, applyBookBlockSelection } = opts
  const { onBlockIndent, onBlockOutdent, onBlockCancel } =
    useBookLayoutMutations(opts)
  const ai = useBookLayoutAiReorganize(opts.notebookId, bookBlocks)

  async function confirmAiReorganize() {
    const mutation = await ai.confirmSuggest()
    if (mutation) {
      opts.onBookUpdated(bookFullAfterLayoutMutation(opts.getBook(), mutation))
    }
  }

  const currentBlockForNavBar = computed(() => {
    const curId = opts.currentBlockId.value
    const selId = selectedBlockId.value
    if (curId === null || selId === null || curId === selId) return null
    return bookBlocks.value.find((b) => b.id === curId) ?? null
  })

  async function readFromHere() {
    const block = currentBlockForNavBar.value
    if (block) await applyBookBlockSelection(block)
  }

  async function backToSelected() {
    const selId = selectedBlockId.value
    if (selId === null) return
    const block = bookBlocks.value.find((b) => b.id === selId)
    if (block) await applyBookBlockSelection(block)
  }

  return {
    layoutListeners: {
      blockIndent: onBlockIndent,
      blockOutdent: onBlockOutdent,
      blockCancel: onBlockCancel,
      requestAiReorganize: ai.requestSuggest,
    },
    aiSuggestion: ai.suggestion,
    aiPreviewRows: ai.previewRows,
    confirmAiReorganize,
    dismissAiReorganizePreview: ai.dismiss,
    currentBlockForNavBar,
    readFromHere,
    backToSelected,
  }
}

export type BookReadingSession = ReturnType<typeof useBookReadingSession>
