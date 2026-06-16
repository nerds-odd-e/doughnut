import { useBookLayoutMutations } from "@/composables/book-reading/useBookLayoutMutations"
import { teardownGlobalClientForTesting } from "@/managedApi/clientSetup"
import type { BookBlockFull, BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import helper, { wrapSdkResponse } from "@tests/helpers"
import GlobalApiLoadingModal from "@tests/helpers/GlobalApiLoadingModal"
import makeMe from "doughnut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { afterEach, describe, expect, it, vi } from "vitest"
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
          getPropBook: () => updatedBook.value,
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

    let resolveIndent: (
      value: ReturnType<
        typeof NotebookBooksController.changeBookBlockDepth
      > extends Promise<infer R>
        ? R
        : never
    ) => void = () => undefined

    vi.spyOn(
      NotebookBooksController,
      "changeBookBlockDepth"
    ).mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveIndent = resolve
        }) as ReturnType<typeof NotebookBooksController.changeBookBlockDepth>
    )

    const wrapper = mountMutationsHarness(book)
    await flushPromises()

    await wrapper.find('[data-testid="indent"]').trigger("click")
    await flushPromises()

    expect(loadingModal()).toBeTruthy()
    expect(document.body.textContent).toContain("Updating book layout…")

    resolveIndent(
      wrapSdkResponse({
        ...book,
        blocks: [{ id: 1, depth: 1, title: "A", contentLocators: [] }],
      })
    )
    await flushPromises()

    expect(loadingModal()).toBeNull()
    wrapper.unmount()
  })
})
