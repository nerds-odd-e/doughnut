import BookReadingContentStreamPanel from "@/components/book-reading/BookReadingContentStreamPanel.vue"
import { BOOK_READING_CONTENT_BLOCK_PREVIEW_MAX_CHARS } from "@/lib/book-reading/contentBlockRawPreview"
import helper from "@tests/helpers"
import type { BookContentBlockFull } from "@generated/doughnut-backend-api"
import { describe, expect, it } from "vitest"

function row(
  partial: Pick<BookContentBlockFull, "id" | "type" | "raw"> & {
    pageIdx?: number
  }
): BookContentBlockFull {
  return {
    id: partial.id,
    type: partial.type,
    raw: partial.raw,
    ...(partial.pageIdx !== undefined ? { pageIdx: partial.pageIdx } : {}),
  }
}

describe("BookReadingContentStreamPanel", () => {
  it("renders a row per content block with stable ids and text preview", () => {
    const blocks = [
      row({
        id: 10,
        type: "text",
        raw: JSON.stringify({ text: "First paragraph." }),
      }),
      row({
        id: 11,
        type: "text",
        raw: JSON.stringify({ text: "Second paragraph." }),
      }),
    ]
    const wrapper = helper
      .component(BookReadingContentStreamPanel)
      .withProps({
        contentBlocks: blocks,
        selectedBlockTitle: "Chapter 1",
      })
      .mount()

    const items = wrapper.findAll('[data-testid="book-reading-content-block"]')
    expect(items).toHaveLength(2)
    expect(items[0]!.attributes("data-book-content-block-id")).toBe("10")
    expect(items[1]!.attributes("data-book-content-block-id")).toBe("11")
    expect(items[0]!.text()).toContain("First paragraph.")
    expect(items[1]!.text()).toContain("Second paragraph.")
    expect(
      wrapper.find('[data-testid="book-reading-content-stream"]').exists()
    ).toBe(true)
    expect(wrapper.text()).toContain("Chapter 1")
  })

  it("uses type label when raw JSON has no text field", () => {
    const wrapper = helper
      .component(BookReadingContentStreamPanel)
      .withProps({
        contentBlocks: [
          row({
            id: 3,
            type: "image",
            raw: JSON.stringify({ img_path: "x.png" }),
          }),
        ],
      })
      .mount()

    const item = wrapper.find('[data-testid="book-reading-content-block"]')
    expect(item.attributes("data-book-content-block-id")).toBe("3")
    expect(item.text()).toBe("[image]")
  })

  it("falls back for invalid JSON to trimmed raw", () => {
    const wrapper = helper
      .component(BookReadingContentStreamPanel)
      .withProps({
        contentBlocks: [
          row({
            id: 4,
            type: "text",
            raw: "not-json",
          }),
        ],
      })
      .mount()

    expect(
      wrapper.find('[data-testid="book-reading-content-block"]').text()
    ).toBe("not-json")
  })

  it("truncates long previews", () => {
    const long = "x".repeat(BOOK_READING_CONTENT_BLOCK_PREVIEW_MAX_CHARS + 40)
    const wrapper = helper
      .component(BookReadingContentStreamPanel)
      .withProps({
        contentBlocks: [
          row({
            id: 5,
            type: "text",
            raw: JSON.stringify({ text: long }),
          }),
        ],
      })
      .mount()

    const text = wrapper
      .find('[data-testid="book-reading-content-block"]')
      .text()
    expect(text.endsWith("…")).toBe(true)
    expect(text.length).toBeLessThanOrEqual(
      BOOK_READING_CONTENT_BLOCK_PREVIEW_MAX_CHARS + 1
    )
  })

  it("shows empty copy when there are no content blocks", () => {
    const wrapper = helper
      .component(BookReadingContentStreamPanel)
      .withProps({ contentBlocks: [] })
      .mount()

    expect(
      wrapper.findAll('[data-testid="book-reading-content-block"]')
    ).toHaveLength(0)
    expect(wrapper.text()).toContain("No imported body for this section.")
  })
})
