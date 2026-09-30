import { type ComputedRef, type Ref, computed, ref } from "vue"
import type {
  BookBlockFull,
  BookFull,
  BookMutationResponseFull,
} from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { applyBookLayoutDepths } from "@/composables/book-reading/applyBookLayoutDepths"
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
  getBook: () => BookFull
  selectedBlockId: Ref<number | null>
  applyBookBlockSelection: (block: BookBlockFull) => Promise<void>
  onBookUpdated: (book: BookFull) => void
}) {
  const layoutMutationInFlight = ref(false)
  const lastDepthChange = ref<{
    before: Map<number, number>
    after: Map<number, number>
  } | null>(null)

  const depthsById = (blocks: BookBlockFull[]) =>
    new Map(blocks.map((b) => [b.id, b.depth] as const))

  const canUndoDepthChange = computed(() => {
    const last = lastDepthChange.value
    if (!last) return false
    const current = depthsById(opts.bookBlocks.value)
    return (
      current.size === last.after.size &&
      [...last.after].every(([id, depth]) => current.get(id) === depth)
    )
  })

  async function changeBlockDepth(
    block: BookBlockFull,
    direction: "INDENT" | "OUTDENT"
  ) {
    if (layoutMutationInFlight.value) {
      return
    }
    layoutMutationInFlight.value = true
    try {
      const before = depthsById(opts.bookBlocks.value)
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
        const updated = bookFullAfterLayoutMutation(opts.getBook(), data)
        lastDepthChange.value = { before, after: depthsById(updated.blocks) }
        opts.onBookUpdated(updated)
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

  async function onUndoDepthChange() {
    const last = lastDepthChange.value
    if (!last || !canUndoDepthChange.value || layoutMutationInFlight.value) {
      return
    }
    layoutMutationInFlight.value = true
    try {
      const data = await applyBookLayoutDepths(opts.notebookId.value, {
        blocks: [...last.before].map(([id, depth]) => ({ id, depth })),
      })
      if (data) {
        lastDepthChange.value = null
        opts.onBookUpdated(bookFullAfterLayoutMutation(opts.getBook(), data))
      }
    } finally {
      layoutMutationInFlight.value = false
    }
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
        const merged = bookFullAfterLayoutMutation(opts.getBook(), data)
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

  return {
    onBlockIndent,
    onBlockOutdent,
    onBlockCancel,
    canUndoDepthChange,
    onUndoDepthChange,
  }
}
