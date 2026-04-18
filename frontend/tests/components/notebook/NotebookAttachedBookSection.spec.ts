import { client } from "@generated/doughnut-backend-api/client.gen"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import NotebookAttachedBookSection from "@/components/notebook/NotebookAttachedBookSection.vue"
import usePopups from "@/components/commons/Popups/usePopups"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { nextTick } from "vue"

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

  it("shows format-neutral attached description for an EPUB book", async () => {
    const attached = makeMe.aBook.bookName("Chapters").format("epub").please()
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(attached)
    )

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    const desc = wrapper.find(".section-description")
    expect(desc.text()).not.toMatch(/PDF/i)
    expect(desc.text()).toContain("attached book")
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
    expect(stack?.[0]?.message).toContain("attached book file")
    expect(stack?.[0]?.message).toContain("cannot be undone")
    expect(stack?.[0]?.message).not.toMatch(/PDF/i)
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

  it("posts multipart attach-book with epub metadata and reloads when a .epub file is chosen", async () => {
    const attached = makeMe.aBook
      .bookName("My Epub")
      .format("epub")
      .notebookId(String(notebookId))
      .please()
    vi.spyOn(NotebookBooksController, "getBook")
      .mockResolvedValueOnce(
        wrapSdkError("nf") as Awaited<
          ReturnType<typeof NotebookBooksController.getBook>
        >
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(attached) as Awaited<
          ReturnType<typeof NotebookBooksController.getBook>
        >
      )
    const postSpy = vi
      .spyOn(client, "post")
      .mockResolvedValue(
        wrapSdkResponse(attached) as Awaited<ReturnType<typeof client.post>>
      )

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    const file = new File(["epub"], "My Epub.epub", {
      type: "application/epub+zip",
    })
    const input = wrapper.find('input[type="file"]').element as HTMLInputElement
    Object.defineProperty(input, "files", {
      value: [file],
      writable: false,
      configurable: true,
    })
    await wrapper.find('input[type="file"]').trigger("change")
    await flushPromises()

    expect(postSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        url: "/api/notebooks/{notebook}/attach-book",
        path: { notebook: notebookId },
      })
    )
    const posted = postSpy.mock.calls[0]![0] as { body: FormData }
    expect(posted.body).toBeInstanceOf(FormData)
    expect(NotebookBooksController.getBook).toHaveBeenCalledTimes(2)
    expect(
      wrapper.find('[data-testid="notebook-attached-book"]').exists()
    ).toBe(true)
  })

  it("shows LoadingModal while EPUB attach-book is in progress", async () => {
    const attached = makeMe.aBook
      .bookName("Deferred Epub")
      .format("epub")
      .notebookId(String(notebookId))
      .please()
    vi.spyOn(NotebookBooksController, "getBook")
      .mockResolvedValueOnce(
        wrapSdkError("nf") as Awaited<
          ReturnType<typeof NotebookBooksController.getBook>
        >
      )
      .mockResolvedValueOnce(
        wrapSdkResponse(attached) as Awaited<
          ReturnType<typeof NotebookBooksController.getBook>
        >
      )
    let resolvePost: () => void
    const postHeld = new Promise<void>((r) => {
      resolvePost = r
    })
    vi.spyOn(client, "post").mockImplementation(async () => {
      await postHeld
      return wrapSdkResponse(attached) as Awaited<
        ReturnType<typeof client.post>
      >
    })

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    const file = new File(["epub"], "Deferred Epub.epub", {
      type: "application/epub+zip",
    })
    const input = wrapper.find('input[type="file"]').element as HTMLInputElement
    Object.defineProperty(input, "files", {
      value: [file],
      writable: false,
      configurable: true,
    })
    await wrapper.find('input[type="file"]').trigger("change")
    await nextTick()

    expect(document.querySelector(".loading-modal-mask")).toBeTruthy()
    expect(document.body.textContent).toContain("Uploading book…")
    resolvePost!()
    await flushPromises()
    expect(document.querySelector(".loading-modal-mask")).toBeNull()
  })

  it("does not post attach-book when a .pdf file is chosen (frontend EPUB only)", async () => {
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkError("nf") as Awaited<
        ReturnType<typeof NotebookBooksController.getBook>
      >
    )
    const postSpy = vi.spyOn(client, "post")

    const wrapper = helper
      .component(NotebookAttachedBookSection)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    const file = new File(["%PDF"], "Report.pdf", { type: "application/pdf" })
    const input = wrapper.find('input[type="file"]').element as HTMLInputElement
    Object.defineProperty(input, "files", {
      value: [file],
      writable: false,
      configurable: true,
    })
    await wrapper.find('input[type="file"]').trigger("change")
    await flushPromises()

    expect(postSpy).not.toHaveBeenCalled()
    expect(NotebookBooksController.getBook).toHaveBeenCalledTimes(1)
    expect(wrapper.find('[data-testid="notebook-no-book"]').exists()).toBe(true)
  })
})
