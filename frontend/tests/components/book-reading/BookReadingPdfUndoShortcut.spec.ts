import {
  setupGlobalClient,
  teardownGlobalClientForTesting,
} from "@/managedApi/clientSetup"
import type { BookFull } from "@generated/donut-backend-api"
import { NotebookBooksController } from "@generated/donut-backend-api/sdk.gen"
import { wrapSdkResponse } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  bookReadingPdfProps,
  mountBookReadingPdf,
} from "./bookReadingPdfAiReorganizeTestSupport"

vi.mock("vue-toastification", () => ({
  useToast: () => ({ error: vi.fn() }),
}))

describe("BookReadingPdf undo shortcut", () => {
  const blockRow = (
    wrapper: ReturnType<typeof mountBookReadingPdf>,
    index: number
  ) => wrapper.findAll('[data-testid="book-reading-book-block"]')[index]!

  const pressUndo = (target: EventTarget = document.body) => {
    const event = new KeyboardEvent("keydown", {
      key: "z",
      ctrlKey: true,
      bubbles: true,
      cancelable: true,
    })
    target.dispatchEvent(event)
    return event
  }

  const indentSecondBlock = async (
    wrapper: ReturnType<typeof mountBookReadingPdf>
  ) => {
    await blockRow(wrapper, 1).trigger("keydown", {
      key: "ArrowRight",
      altKey: true,
      shiftKey: true,
    })
    await flushPromises()
    await wrapper.setProps({
      book: wrapper.emitted("update:book")![0]![0] as BookFull,
    })
  }

  const mountWithIndentedBlock = async () => {
    const book = makeMe.aBook
      .notebookId("9")
      .blocks([
        makeMe.aBookBlock.id(1).depth(0).title("A").please(),
        makeMe.aBookBlock.id(2).depth(0).title("B").please(),
      ])
      .please()
    vi.spyOn(NotebookBooksController, "changeBookBlockDepth").mockResolvedValue(
      wrapSdkResponse({
        ...book,
        blocks: [
          { id: 1, depth: 0, title: "A", contentLocators: [] },
          { id: 2, depth: 1, title: "B", contentLocators: [] },
        ],
      })
    )
    const apply = vi
      .spyOn(NotebookBooksController, "applyBookLayoutReorganization")
      .mockResolvedValue(wrapSdkResponse(book))
    const wrapper = mountBookReadingPdf(bookReadingPdfProps(book))
    await flushPromises()
    return { wrapper, apply }
  }

  beforeEach(() => {
    setupGlobalClient({ states: [] })
    vi.spyOn(
      NotebookBooksController,
      "getNotebookBookReadingRecords"
    ).mockResolvedValue(wrapSdkResponse([]))
  })

  afterEach(() => {
    teardownGlobalClientForTesting()
  })

  it("undoes the last depth change when Ctrl+Z is pressed on the page", async () => {
    const { wrapper, apply } = await mountWithIndentedBlock()
    await indentSecondBlock(wrapper)

    const event = pressUndo()
    await flushPromises()

    expect(apply).toHaveBeenCalledWith(
      expect.objectContaining({
        body: {
          blocks: [
            { id: 1, depth: 0 },
            { id: 2, depth: 0 },
          ],
        },
      })
    )
    expect(event.defaultPrevented).toBe(true)
    wrapper.unmount()
  })

  it("does nothing when there is no depth change to undo", async () => {
    const { wrapper, apply } = await mountWithIndentedBlock()

    const event = pressUndo()
    await flushPromises()

    expect(apply).not.toHaveBeenCalled()
    expect(event.defaultPrevented).toBe(false)
    wrapper.unmount()
  })

  it("leaves Ctrl+Z to a text input", async () => {
    const { wrapper, apply } = await mountWithIndentedBlock()
    await indentSecondBlock(wrapper)
    const input = document.createElement("input")
    document.body.appendChild(input)

    const event = pressUndo(input)
    await flushPromises()

    expect(apply).not.toHaveBeenCalled()
    expect(event.defaultPrevented).toBe(false)
    input.remove()
    wrapper.unmount()
  })

  it("stops listening once the reading page is unmounted", async () => {
    const { wrapper, apply } = await mountWithIndentedBlock()
    await indentSecondBlock(wrapper)
    wrapper.unmount()

    pressUndo()
    await flushPromises()

    expect(apply).not.toHaveBeenCalled()
  })
})
