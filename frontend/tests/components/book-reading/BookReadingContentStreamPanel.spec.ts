import BookReadingContentStreamPanel from "@/components/book-reading/BookReadingContentStreamPanel.vue"
import { BOOK_READING_CONTENT_BLOCK_PREVIEW_MAX_CHARS } from "@/lib/book-reading/contentBlockRawPreview"
import { BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS } from "@/lib/book-reading/contentBlockStructuralTitleSource"
import helper from "@tests/helpers"
import type { BookContentBlockFull } from "@generated/doughnut-backend-api"
import { describe, expect, it, vi } from "vitest"

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

  describe("long-press callout", () => {
    function twoBlocks() {
      return [
        row({
          id: 20,
          type: "text",
          raw: JSON.stringify({ text: "Block A." }),
        }),
        row({
          id: 21,
          type: "text",
          raw: JSON.stringify({ text: "Block B." }),
        }),
      ]
    }

    function pointerDown(el: Element, x = 50, y = 50, pointerId = 1) {
      el.dispatchEvent(
        new PointerEvent("pointerdown", {
          bubbles: true,
          cancelable: true,
          pointerId,
          pointerType: "touch",
          button: 0,
          clientX: x,
          clientY: y,
        })
      )
    }

    function pointerMove(el: Element, x: number, y: number, pointerId = 1) {
      el.dispatchEvent(
        new PointerEvent("pointermove", {
          bubbles: true,
          cancelable: true,
          pointerId,
          pointerType: "touch",
          clientX: x,
          clientY: y,
        })
      )
    }

    it("shows callout after pointer hold timer fires", async () => {
      vi.useFakeTimers()
      const wrapper = helper
        .component(BookReadingContentStreamPanel)
        .withProps({ contentBlocks: twoBlocks() })
        .mount({ attachTo: document.body })

      const items = wrapper.findAll(
        '[data-testid="book-reading-content-block"]'
      )
      pointerDown(items[0]!.element)
      expect(
        wrapper
          .find('[data-testid="book-reading-content-new-block-confirm"]')
          .exists()
      ).toBe(false)

      vi.advanceTimersByTime(500)
      await wrapper.vm.$nextTick()

      expect(
        wrapper
          .find('[data-testid="book-reading-content-new-block-confirm"]')
          .exists()
      ).toBe(true)
      vi.useRealTimers()
    })

    it("cancels hold if pointer moves beyond tolerance before timer fires", async () => {
      vi.useFakeTimers()
      const wrapper = helper
        .component(BookReadingContentStreamPanel)
        .withProps({ contentBlocks: twoBlocks() })
        .mount({ attachTo: document.body })

      const item = wrapper.findAll(
        '[data-testid="book-reading-content-block"]'
      )[0]!
      pointerDown(item.element, 50, 50)
      pointerMove(item.element, 70, 50)
      vi.advanceTimersByTime(500)
      await wrapper.vm.$nextTick()

      expect(
        wrapper
          .find('[data-testid="book-reading-content-new-block-confirm"]')
          .exists()
      ).toBe(false)
      vi.useRealTimers()
    })

    it("dismisses callout on Cancel click", async () => {
      vi.useFakeTimers()
      const wrapper = helper
        .component(BookReadingContentStreamPanel)
        .withProps({ contentBlocks: twoBlocks() })
        .mount({ attachTo: document.body })

      const item = wrapper.findAll(
        '[data-testid="book-reading-content-block"]'
      )[0]!
      pointerDown(item.element)
      vi.advanceTimersByTime(500)
      await wrapper.vm.$nextTick()

      await wrapper
        .find('[data-testid="book-reading-content-new-block-cancel"]')
        .trigger("click")

      expect(
        wrapper
          .find('[data-testid="book-reading-content-new-block-confirm"]')
          .exists()
      ).toBe(false)
      vi.useRealTimers()
    })

    it("emits createBlockFromContent with correct id when New block is clicked", async () => {
      vi.useFakeTimers()
      const wrapper = helper
        .component(BookReadingContentStreamPanel)
        .withProps({ contentBlocks: twoBlocks() })
        .mount({ attachTo: document.body })

      const item = wrapper.findAll(
        '[data-testid="book-reading-content-block"]'
      )[0]!
      pointerDown(item.element)
      vi.advanceTimersByTime(500)
      await wrapper.vm.$nextTick()

      await wrapper
        .find('[data-testid="book-reading-content-new-block-confirm"]')
        .trigger("click")

      expect(wrapper.emitted("createBlockFromContent")).toEqual([
        [{ contentBlockId: 20 }],
      ])
      vi.useRealTimers()
    })

    it("opens title modal when New block is confirmed on long text, then emits with structuralTitle", async () => {
      vi.useFakeTimers()
      const longText = "Z".repeat(BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS + 8)
      const wrapper = helper
        .component(BookReadingContentStreamPanel)
        .withProps({
          contentBlocks: [
            row({
              id: 30,
              type: "text",
              raw: JSON.stringify({ text: longText }),
            }),
          ],
        })
        .mount({ attachTo: document.body })

      const item = wrapper.find('[data-testid="book-reading-content-block"]')
      pointerDown(item.element)
      vi.advanceTimersByTime(500)
      await wrapper.vm.$nextTick()

      await wrapper
        .find('[data-testid="book-reading-content-new-block-confirm"]')
        .trigger("click")
      await wrapper.vm.$nextTick()

      const dialog = wrapper.find(
        '[data-testid="book-reading-new-block-title-dialog"]'
      )
      expect(dialog.classes()).toContain("daisy-modal-open")
      const input = wrapper.find(
        '[data-testid="book-reading-new-block-title-input"]'
      )
      expect(input.element).toBeInstanceOf(HTMLInputElement)
      expect((input.element as HTMLInputElement).value).toHaveLength(
        BOOK_BLOCK_STRUCTURAL_TITLE_MAX_CHARS
      )

      await input.setValue("Short custom title")
      await wrapper
        .find('[data-testid="book-reading-new-block-title-confirm"]')
        .trigger("click")
      await wrapper.vm.$nextTick()

      expect(wrapper.emitted("createBlockFromContent")).toEqual([
        [{ contentBlockId: 30, structuralTitle: "Short custom title" }],
      ])
      vi.useRealTimers()
    })

    it("does not show callout when disabled", async () => {
      vi.useFakeTimers()
      const wrapper = helper
        .component(BookReadingContentStreamPanel)
        .withProps({ contentBlocks: twoBlocks(), disabled: true })
        .mount({ attachTo: document.body })

      const item = wrapper.findAll(
        '[data-testid="book-reading-content-block"]'
      )[0]!
      pointerDown(item.element)
      vi.advanceTimersByTime(500)
      await wrapper.vm.$nextTick()

      expect(
        wrapper
          .find('[data-testid="book-reading-content-new-block-confirm"]')
          .exists()
      ).toBe(false)
      vi.useRealTimers()
    })
  })
})
