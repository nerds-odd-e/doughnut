import { useBookLayoutMutations } from "@/composables/book-reading/useBookLayoutMutations"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import type { BookBlockFull, BookFull } from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import helper, { mockSdkServiceWithImplementation } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it } from "vitest"
import { computed, defineComponent, ref } from "vue"

function blockStub(
  p: Pick<BookBlockFull, "id" | "depth" | "title">
): BookBlockFull {
  return { ...p, contentLocators: [], contentBlocks: [] }
}

describe("useBookLayoutMutations", () => {
  const loadingModal = () => document.querySelector(".loading-modal-mask")

  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  const mountMutationsHarness = (book: BookFull) => {
    const Host = defineComponent({
      components: { GlobalApiLoadingModal },
      setup() {
        const notebookId = computed(() => Number(book.notebookId))
        const bookBlocks = computed(() => book.blocks)
        const selectedBlockId = ref<number | null>(null)
        const updatedBook = ref(book)
        const { onBlockIndent } = useBookLayoutMutations({
          notebookId,
          bookBlocks,
          getBook: () => updatedBook.value,
          selectedBlockId,
          applyBookBlockSelection: async () => undefined,
          onBookUpdated: (next) => {
            updatedBook.value = next
          },
        })
        return {
          onBlockIndent,
          block: book.blocks[0]!,
        }
      },
      template: `
        <button data-testid="indent" @click="onBlockIndent(block)">Indent</button>
        <GlobalApiLoadingModal />
      `,
    })

    return helper.component(Host).mount({ attachTo: document.body })
  }

  it("shows the global loading modal while indent API is pending", async () => {
    const book = makeMe.aBook
      .notebookId("9")
      .blocks([blockStub({ id: 1, depth: 0, title: "A" })])
      .please()

    let resolveIndent: (value: BookFull) => void = () => undefined

    mockSdkServiceWithImplementation(
      NotebookBooksController,
      "changeBookBlockDepth",
      () =>
        new Promise((resolve) => {
          resolveIndent = resolve
        })
    )

    const wrapper = mountMutationsHarness(book)
    await flushPromises()

    await wrapper.find('[data-testid="indent"]').trigger("click")
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Updating book layout…")

    resolveIndent({
      ...book,
      blocks: [
        { id: 1, depth: 1, title: "A", contentLocators: [], contentBlocks: [] },
      ],
    })
    await flushPromises()

    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  })

  describe("undo of the last depth change", () => {
    const layout = () => [
      blockStub({ id: 1, depth: 0, title: "A" }),
      blockStub({ id: 2, depth: 1, title: "A.1" }),
      blockStub({ id: 3, depth: 1, title: "A.2" }),
    ]

    const mountUndoHarness = () => {
      const book = makeMe.aBook.notebookId("9").blocks(layout()).please()
      const depths = (b: BookFull) => b.blocks.map((x) => x.depth)
      const Host = defineComponent({
        setup() {
          const current = ref<BookFull>(book)
          const m = useBookLayoutMutations({
            notebookId: computed(() => 9),
            bookBlocks: computed(() => current.value.blocks),
            getBook: () => current.value,
            selectedBlockId: ref<number | null>(null),
            applyBookBlockSelection: async () => undefined,
            onBookUpdated: (next) => {
              current.value = next
            },
          })
          return { ...m, current, blocks: current.value.blocks }
        },
        template: "<div />",
      })
      const wrapper = helper.component(Host).mount()
      return { vm: wrapper.vm, depths }
    }

    const respondWith = (
      method:
        | "changeBookBlockDepth"
        | "cancelBookBlock"
        | "applyBookLayoutReorganization",
      blocks: [number, number][]
    ) =>
      mockSdkServiceWithImplementation(
        NotebookBooksController,
        method,
        async () =>
          ({
            blocks: blocks.map(([id, depth]) => ({
              id,
              depth,
              title: `b${id}`,
              contentLocators: [],
            })),
          }) as never
      )

    it("restores the depths before an outdent", async () => {
      const { vm, depths } = mountUndoHarness()
      respondWith("changeBookBlockDepth", [
        [1, 0],
        [2, 0],
        [3, 1],
      ])
      await vm.onBlockOutdent(vm.blocks[1]!)
      expect(vm.canUndoDepthChange).toBe(true)

      const apply = respondWith("applyBookLayoutReorganization", [
        [1, 0],
        [2, 1],
        [3, 1],
      ])
      await vm.onUndoDepthChange()

      expect(apply).toHaveBeenCalledWith(
        expect.objectContaining({
          body: {
            blocks: [
              { id: 1, depth: 0 },
              { id: 2, depth: 1 },
              { id: 3, depth: 1 },
            ],
          },
        })
      )
      expect(depths(vm.current)).toEqual([0, 1, 1])
      expect(vm.canUndoDepthChange).toBe(false)
    })

    it("restores the depths before an indent", async () => {
      const { vm, depths } = mountUndoHarness()
      respondWith("changeBookBlockDepth", [
        [1, 1],
        [2, 2],
        [3, 2],
      ])
      await vm.onBlockIndent(vm.blocks[0]!)
      expect(vm.canUndoDepthChange).toBe(true)
      respondWith("applyBookLayoutReorganization", [
        [1, 0],
        [2, 1],
        [3, 1],
      ])
      await vm.onUndoDepthChange()
      expect(depths(vm.current)).toEqual([0, 1, 1])
    })

    it("is unavailable after a later cancel changed the depths", async () => {
      const { vm } = mountUndoHarness()
      respondWith("changeBookBlockDepth", [
        [1, 0],
        [2, 0],
        [3, 1],
      ])
      await vm.onBlockOutdent(vm.blocks[1]!)
      respondWith("cancelBookBlock", [
        [1, 0],
        [3, 0],
      ])
      await vm.onBlockCancel(vm.current.blocks[1]!)
      expect(vm.canUndoDepthChange).toBe(false)
    })

    it("offers only the latest depth change after another block changed depth", async () => {
      const { vm } = mountUndoHarness()
      respondWith("changeBookBlockDepth", [
        [1, 0],
        [2, 0],
        [3, 1],
      ])
      await vm.onBlockOutdent(vm.blocks[1]!)
      respondWith("changeBookBlockDepth", [
        [1, 0],
        [2, 0],
        [3, 0],
      ])
      await vm.onBlockOutdent(vm.current.blocks[2]!)
      // the latest change is what undo offers, not the earlier one
      expect(vm.canUndoDepthChange).toBe(true)
    })
  })
})
