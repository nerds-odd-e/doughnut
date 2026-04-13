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

function mountLayout(
  blocks = [blockStub({ id: 1, depth: 0, title: "A" })],
  options?: {
    pendingLayoutBlockId?: number | null
    selectedBlockId?: number | null
    fullLayoutBusy?: boolean
    isMdOrLarger?: boolean
    opened?: boolean
  }
) {
  return helper
    .component(BookReadingBookLayout)
    .withProps({
      opened: options?.opened ?? true,
      panelId,
      isMdOrLarger: options?.isMdOrLarger ?? true,
      blocks,
      currentBlockId: null,
      selectedBlockId: options?.selectedBlockId ?? null,
      pendingLayoutBlockId: options?.pendingLayoutBlockId ?? null,
      fullLayoutBusy: options?.fullLayoutBusy ?? false,
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

  it("emits blockIndent with parent block when dragging right on a parent that has children", () => {
    const parent = blockStub({ id: 10, depth: 0, title: "Parent" })
    const child = blockStub({ id: 11, depth: 1, title: "Child" })
    const wrapper = mountLayout([parent, child])
    const rows = wrapper.findAll('[data-testid="book-reading-book-block"]')
    const parentRow = rows[0]!.element as HTMLElement

    const x0 = 200
    const y0 = 120
    pointerMouse(parentRow, { type: "pointerdown", clientX: x0, clientY: y0 })
    pointerMouse(parentRow, {
      type: "pointermove",
      clientX: x0 + 30,
      clientY: y0,
    })
    pointerMouse(parentRow, {
      type: "pointerup",
      clientX: x0 + 30,
      clientY: y0,
    })

    expect(wrapper.emitted("blockIndent")).toHaveLength(1)
    expect(wrapper.emitted("blockIndent")![0]![0]).toEqual(parent)
    wrapper.unmount()
  })

  it("emits blockOutdent with parent block when dragging left on a parent that has children", () => {
    const parent = blockStub({ id: 12, depth: 1, title: "Parent" })
    const child = blockStub({ id: 13, depth: 2, title: "Child" })
    const wrapper = mountLayout([parent, child])
    const rows = wrapper.findAll('[data-testid="book-reading-book-block"]')
    const parentRow = rows[0]!.element as HTMLElement

    const x0 = 200
    const y0 = 120
    pointerMouse(parentRow, { type: "pointerdown", clientX: x0, clientY: y0 })
    pointerMouse(parentRow, {
      type: "pointermove",
      clientX: x0 - 30,
      clientY: y0,
    })
    pointerMouse(parentRow, {
      type: "pointerup",
      clientX: x0 - 30,
      clientY: y0,
    })

    expect(wrapper.emitted("blockOutdent")).toHaveLength(1)
    expect(wrapper.emitted("blockOutdent")![0]![0]).toEqual(parent)
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

  it("shows pending overlay only on the row matching pendingLayoutBlockId", () => {
    const a = blockStub({ id: 1, depth: 0, title: "A" })
    const b = blockStub({ id: 2, depth: 0, title: "B" })
    const wrapper = mountLayout([a, b], { pendingLayoutBlockId: 2 })
    const rows = wrapper.findAll('[data-testid="book-reading-book-block"]')
    expect(
      rows[0]!
        .find('[data-testid="book-reading-book-block-layout-pending"]')
        .exists()
    ).toBe(false)
    expect(
      rows[1]!
        .find('[data-testid="book-reading-book-block-layout-pending"]')
        .exists()
    ).toBe(true)
    expect(
      (rows[1]!.element as HTMLButtonElement).getAttribute("aria-busy")
    ).toBe("true")
    wrapper.unmount()
  })

  it("does not emit blockIndent when layout is locked by pendingLayoutBlockId", () => {
    const block = blockStub({ id: 1, depth: 0, title: "A" })
    const wrapper = mountLayout([block], { pendingLayoutBlockId: 1 })
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

    expect(wrapper.emitted("blockIndent")).toBeUndefined()
    wrapper.unmount()
  })

  it("does not emit blockClick when layout is locked", async () => {
    const block = blockStub({ id: 3, depth: 0, title: "C" })
    const wrapper = mountLayout([block], { pendingLayoutBlockId: 3 })
    const row = wrapper.find('[data-testid="book-reading-book-block"]')
    await row.trigger("click")
    expect(wrapper.emitted("blockClick")).toBeUndefined()
    wrapper.unmount()
  })

  it("shows full-layout busy overlay and spinner when fullLayoutBusy", () => {
    const wrapper = mountLayout(undefined, { fullLayoutBusy: true })
    const busy = wrapper.find('[data-testid="book-reading-layout-full-busy"]')
    expect(busy.exists()).toBe(true)
    expect(busy.find(".daisy-loading-spinner").exists()).toBe(true)
    wrapper.unmount()
  })

  it("shows aside busy overlay on small viewports when fullLayoutBusy and opened", () => {
    const wrapper = mountLayout(undefined, {
      fullLayoutBusy: true,
      isMdOrLarger: false,
      opened: true,
    })
    expect(
      wrapper
        .find('[data-testid="book-reading-layout-aside-full-busy"]')
        .exists()
    ).toBe(true)
    wrapper.unmount()
  })

  it("disables AI Reorganize when fullLayoutBusy", () => {
    const wrapper = mountLayout(undefined, { fullLayoutBusy: true })
    const btn = wrapper.find(
      '[data-testid="book-reading-ai-reorganize-layout"]'
    ).element as HTMLButtonElement
    expect(btn.disabled).toBe(true)
    wrapper.unmount()
  })

  it("emits requestAiReorganize when AI Reorganize is clicked", async () => {
    const wrapper = mountLayout()
    await wrapper
      .find('[data-testid="book-reading-ai-reorganize-layout"]')
      .trigger("click")
    expect(wrapper.emitted("requestAiReorganize")).toHaveLength(1)
    wrapper.unmount()
  })

  it("does not emit blockIndent when fullLayoutBusy", () => {
    const block = blockStub({ id: 1, depth: 0, title: "A" })
    const wrapper = mountLayout([block], { fullLayoutBusy: true })
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

    expect(wrapper.emitted("blockIndent")).toBeUndefined()
    wrapper.unmount()
  })

  it("does not emit blockClick when fullLayoutBusy", async () => {
    const block = blockStub({ id: 3, depth: 0, title: "C" })
    const wrapper = mountLayout([block], { fullLayoutBusy: true })
    const row = wrapper.find('[data-testid="book-reading-book-block"]')
    await row.trigger("click")
    expect(wrapper.emitted("blockClick")).toBeUndefined()
    wrapper.unmount()
  })

  it("does not emit indent, outdent, or cancel from keyboard when layout is locked", async () => {
    const block = blockStub({ id: 7, depth: 1, title: "Locked" })
    const wrapper = mountLayout([block], {
      pendingLayoutBlockId: 7,
      selectedBlockId: 7,
    })
    const row = wrapper.find('[data-testid="book-reading-book-block"]')
    const el = row.element as HTMLElement
    el.focus()

    el.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Tab",
        bubbles: true,
        cancelable: true,
      })
    )
    el.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Tab",
        shiftKey: true,
        bubbles: true,
        cancelable: true,
      })
    )
    el.dispatchEvent(
      new KeyboardEvent("keydown", {
        key: "Delete",
        bubbles: true,
        cancelable: true,
      })
    )

    expect(wrapper.emitted("blockIndent")).toBeUndefined()
    expect(wrapper.emitted("blockOutdent")).toBeUndefined()
    expect(wrapper.emitted("blockCancel")).toBeUndefined()
    wrapper.unmount()
  })
})
