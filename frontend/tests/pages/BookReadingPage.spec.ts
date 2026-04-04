import type { BookFull } from "@generated/doughnut-backend-api"
import { NotebookBooksController } from "@generated/doughnut-backend-api/sdk.gen"
import BookReadingPage from "@/pages/BookReadingPage.vue"
import helper, { wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"

describe("BookReadingPage", () => {
  const notebookId = 7

  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it("shows download link when hasSourceFile is true", async () => {
    const book: BookFull = {
      id: 1,
      bookName: "Linear Algebra",
      format: "pdf",
      hasSourceFile: true,
      ranges: [],
    }
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(book)
    )

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    const link = wrapper.find('[data-testid="book-download-pdf"]')
    expect(link.exists()).toBe(true)
    expect(link.attributes("href")).toBe(
      `/api/notebooks/${notebookId}/book/file`
    )
    expect(link.text()).toContain("Download")
  })

  it("hides download link when hasSourceFile is false", async () => {
    const book: BookFull = {
      id: 1,
      bookName: "Linear Algebra",
      format: "pdf",
      hasSourceFile: false,
      ranges: [],
    }
    vi.spyOn(NotebookBooksController, "getBook").mockResolvedValue(
      wrapSdkResponse(book)
    )

    const wrapper = helper
      .component(BookReadingPage)
      .withRouter()
      .withProps({ notebookId })
      .mount()
    await flushPromises()

    expect(wrapper.find('[data-testid="book-download-pdf"]').exists()).toBe(
      false
    )
  })
})
