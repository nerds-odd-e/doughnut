import { type ComputedRef, type Ref, ref } from "vue"
import type {
  BookBlockFull,
  BookFull,
  BookMutationResponseFull,
} from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { predecessorBookBlockIdInPreorder } from "@/lib/book-reading/predecessorBookBlockIdInPreorder"

export function bookFullAfterLayoutMutation(
  previous: BookFull,
  mutation: BookMutationResponseFull
): BookFull {
  const prevById = new Map(previous.blocks.map((b) => [b.id, b]))
  const updatedBlocks = mutation.blocks.map((row) => {
    const prev = prevById.get(row.id)
    if (!prev) {
      throw new Error(`book layout mutation: unknown block id ${row.id}`)
    }
    return {
      ...prev,
      id: row.id,
      depth: row.depth,
      title: row.title,
      contentLocators: row.contentLocators ?? prev.contentLocators,
    }
  })
  return { ...previous, ...mutation, blocks: updatedBlocks }
}

const BOOK_LAYOUT_MUTATION_LOADING_MESSAGE = "Updating book layout…"

export function useBookLayoutMutations(opts: {
  notebookId: ComputedRef<number>
  bookBlocks: ComputedRef<BookBlockFull[]>
  getPropBook: () => BookFull
  selectedBlockId: Ref<number | null>
  applyBookBlockSelection: (block: BookBlockFull) => Promise<void>
  onBookUpdated: (book: BookFull) => void
}) {
  const layoutMutationInFlight = ref(false)

  async function changeBlockDepth(
    block: BookBlockFull,
    direction: "INDENT" | "OUTDENT"
  ) {
    if (layoutMutationInFlight.value) {
      return
    }
    layoutMutationInFlight.value = true
    try {
      const { data, error } = await apiCallWithLoading(
        () =>
          NotebookBooksController.changeBookBlockDepth({
            path: {
              notebook: opts.notebookId.value,
              bookBlock: block.id,
            },
            body: { direction },
          }),
        { blockUi: true, message: BOOK_LAYOUT_MUTATION_LOADING_MESSAGE }
      )
      if (!error && data) {
        opts.onBookUpdated(
          bookFullAfterLayoutMutation(opts.getPropBook(), data)
        )
        opts.selectedBlockId.value = block.id
      }
    } finally {
      layoutMutationInFlight.value = false
    }
  }

  async function onBlockIndent(block: BookBlockFull) {
    await changeBlockDepth(block, "INDENT")
  }

  async function onBlockOutdent(block: BookBlockFull) {
    await changeBlockDepth(block, "OUTDENT")
  }

  async function onBlockCancel(block: BookBlockFull) {
    if (layoutMutationInFlight.value) {
      return
    }
    layoutMutationInFlight.value = true
    try {
      const predecessorId = predecessorBookBlockIdInPreorder(
        opts.bookBlocks.value,
        block.id
      )
      const { data, error } = await apiCallWithLoading(
        () =>
          NotebookBooksController.cancelBookBlock({
            path: { notebook: opts.notebookId.value, bookBlock: block.id },
          }),
        { blockUi: true, message: BOOK_LAYOUT_MUTATION_LOADING_MESSAGE }
      )
      if (!error && data) {
        const merged = bookFullAfterLayoutMutation(opts.getPropBook(), data)
        if (
          predecessorId !== null &&
          merged.blocks.some((b) => b.id === predecessorId)
        ) {
          opts.selectedBlockId.value = predecessorId
          opts.onBookUpdated(merged)
          const pred = merged.blocks.find((b) => b.id === predecessorId)!
          await opts.applyBookBlockSelection(pred)
        } else {
          opts.selectedBlockId.value = null
          opts.onBookUpdated(merged)
        }
      }
    } finally {
      layoutMutationInFlight.value = false
    }
  }

  return { onBlockIndent, onBlockOutdent, onBlockCancel }
}
