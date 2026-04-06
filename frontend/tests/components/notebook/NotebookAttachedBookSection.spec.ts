import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"

describe("NotebookAttachedBookSection", () => {
  const notebookId = 77

  afterEach(() => {
    while (usePopups().popups.peek()?.length) {
      usePopups().popups.done(false)
    }
    vi.restoreAllMocks()
  })

  beforeEach(() => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkError("Not found") as Awaited<
        ReturnType<typeof NotebookBooksController.getBook>
      >
    )
  })

  it("shows Remove book when a book is attached", async () => {
    const attached = makeMe.aBook.bookName("Linear").please()
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(attached)
    )

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    const removeBtn = wrapper.find('button[title="Remove attached book"]')
    expect(removeBtn.exists()).toBe(true)
    expect(removeBtn.text()).toContain("Remove book")
  })

  it("opens confirm popup when Remove book is clicked", async () => {
    const attached = makeMe.aBook.bookName("Linear").please()
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(attached)
    )

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    await wrapper.find('button[title="Remove attached book"]').trigger("click")
    await flushPromises()

    const stack = usePopups().popups.peek()
    expect(stack?.length).toBe(1)
    expect(stack?.[0]?.type).toBe("confirm")
    expect(stack?.[0]?.message).toContain("Linear")
    expect(stack?.[0]?.message).toContain("cannot be undone")
  })

  it("does not call delete when confirm is cancelled", async () => {
    const attached = makeMe.aBook.please()
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(attached)
    )
    const deleteSpy = vi.spyOn(NotebookBooksController, "deleteBook")

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    await wrapper.find('button[title="Remove attached book"]').trigger("click")
    await flushPromises()
    usePopups().popups.done(false)
    await flushPromises()

    expect(deleteSpy).not.toHaveBeenCalled()
  })

  it("calls deleteBook and reloads when confirm is accepted", async () => {
    const attached = makeMe.aBook.please()
    vi.spyOn(NotebookBooksController, "getBook")
      .mockResolvedValueOnce(
        wrapSdkResponse(attached) as Awaited<
          ReturnType<typeof NotebookBooksController.getBook>
        >
      )
      .mockResolvedValueOnce(
        wrapSdkError("gone") as Awaited<
          ReturnType<typeof NotebookBooksController.getBook>
        >
      )
    const deleteSpy = vi
      .spyOn(NotebookBooksController, "deleteBook")
      .mockResolvedValue(
        wrapSdkResponse(undefined) as Awaited<
          ReturnType<typeof NotebookBooksController.deleteBook>
        >
      )

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    await wrapper.find('button[title="Remove attached book"]').trigger("click")
    await flushPromises()
    usePopups().popups.done(true)
    await flushPromises()

    expect(deleteSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
    })
    expect(NotebookBooksController.getBook).toHaveBeenCalledTimes(2)
    expect(wrapper.find('[data-testid="notebook-no-book"]').exists()).toBe(true)
  })
})
