import BookReadingBookLayout from "@/components/book-reading/BookReadingBookLayout.vue"
import helper from "@tests/helpers"
import type { BookBlockFull } from "@generated/doughnut-backend-api"
import { describe, expect, it } from "vitest"

const panelId = "book-reading-book-layout-panel-test"

function blockStub(
  p: Pick<BookBlockFull, "id" | "depth" | "title">
): BookBlockFull {
  return { ...p, allBboxes: [] }
}

function mountLayout(blocks = [blockStub({ id: 1, depth: 0, title: "A" })]) {
  return helper
    .component(BookReadingBookLayout)
    .withProps({
      opened: true,
      panelId,
      isMdOrLarger: true,
      blocks,
      currentBlockId: null,
      selectedBlockId: null,
      dispositionForBlock: () => undefined,
    })
    .mount({ attachTo: document.body })
}

function pointerMouse(
  target: HTMLElement,
  ev: {
    type: "pointerdown" | "pointermove" | "pointerup"
    clientX: number
    clientY: number
    pointerId?: number
  }
) {
  const { type, clientX, clientY, pointerId = 42 } = ev
  const isUp = type === "pointerup"
  target.dispatchEvent(
    new PointerEvent(type, {
      bubbles: true,
      cancelable: true,
      clientX,
      clientY,
      pointerId,
      pointerType: "mouse",
      isPrimary: true,
      button: 0,
      buttons: isUp ? 0 : 1,
    })
  )
}

describe("BookReadingBookLayout", () => {
  it("emits blockIndent after horizontal drag right past threshold", () => {
    const block = blockStub({ id: 1, depth: 0, title: "A" })
    const wrapper = mountLayout([block])
    const row = wrapper.find('[data-testid="book-reading-book-block"]')
      .element as HTMLElement

    const x0 = 200
    const y0 = 120
    pointerMouse(row, { type: "pointerdown", clientX: x0, clientY: y0 })
    pointerMouse(row, {
      type: "pointermove",
      clientX: x0 + 30,
      clientY: y0,
    })
    pointerMouse(row, {
      type: "pointerup",
      clientX: x0 + 30,
      clientY: y0,
    })

    expect(wrapper.emitted("blockIndent")).toHaveLength(1)
    expect(wrapper.emitted("blockIndent")![0]![0]).toEqual(block)
    wrapper.unmount()
  })

  it("emits blockOutdent after horizontal drag left past threshold", () => {
    const block = blockStub({ id: 2, depth: 1, title: "B" })
    const wrapper = mountLayout([block])
    const row = wrapper.find('[data-testid="book-reading-book-block"]')
      .element as HTMLElement

    const x0 = 200
    const y0 = 120
    pointerMouse(row, { type: "pointerdown", clientX: x0, clientY: y0 })
    pointerMouse(row, {
      type: "pointermove",
      clientX: x0 - 30,
      clientY: y0,
    })
    pointerMouse(row, {
      type: "pointerup",
      clientX: x0 - 30,
      clientY: y0,
    })

    expect(wrapper.emitted("blockOutdent")).toHaveLength(1)
    expect(wrapper.emitted("blockOutdent")![0]![0]).toEqual(block)
    wrapper.unmount()
  })

  it("does not emit indent/outdent on click-sized movement and still allows blockClick", async () => {
    const block = blockStub({ id: 3, depth: 0, title: "C" })
    const wrapper = mountLayout([block])
    const row = wrapper.find('[data-testid="book-reading-book-block"]')

    const el = row.element as HTMLElement
    const x0 = 100
    const y0 = 80
    pointerMouse(el, { type: "pointerdown", clientX: x0, clientY: y0 })
    pointerMouse(el, {
      type: "pointerup",
      clientX: x0 + 2,
      clientY: y0 + 1,
    })

    expect(wrapper.emitted("blockIndent")).toBeUndefined()
    expect(wrapper.emitted("blockOutdent")).toBeUndefined()

    await row.trigger("click")
    expect(wrapper.emitted("blockClick")).toHaveLength(1)
    expect(wrapper.emitted("blockClick")![0]![0]).toEqual(block)
    wrapper.unmount()
  })
})
