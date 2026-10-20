import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { userEvent } from "vitest/browser"
import { expandPropertyPanel, propertyRowSelector } from "./propertiesTestDom"
import {
  atViewport,
  DRAFT,
  expectApart,
  expectInside,
  interactiveControls,
  KEY_INPUT,
  PRESET_LIST,
  rect,
  VALUE_INPUT,
} from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const markdown = `---
url: https://a.io
---

Body`

const TRANSPARENT = "rgba(0, 0, 0, 0)"

describe("RichMarkdownEditor property editing layout", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  const mount = () => h.mountEditor(markdown, { attachToBody: true })

  async function startDraft(wrapper: VueWrapper) {
    await h.openAddProperty()
    await new Promise(requestAnimationFrame)
    const keyInput = wrapper.get(`${DRAFT} ${KEY_INPUT}`)
    ;(keyInput.element as HTMLElement).focus()
    await flushPromises()
    return keyInput
  }

  it("keeps idle fields borderless and outlines the field keyboard focus reaches", async () => {
    const wrapper = await mount()
    const row = wrapper.get(propertyRowSelector("url")).element
    const key = row.querySelector<HTMLElement>(KEY_INPUT)!
    const value = row.querySelector<HTMLElement>(VALUE_INPUT)!

    for (const field of [key, value]) {
      const idle = getComputedStyle(field)
      expect(idle.borderTopColor).toBe(TRANSPARENT)
      expect(idle.outlineStyle).toBe("none")
    }

    const keyboardFocus = [
      {
        from: '[data-testid="rich-note-property-panel-toggle"]',
        field: key,
        shift: false,
      },
      {
        from: '[data-testid="rich-note-property-value-dialog-open"]',
        field: value,
        shift: true,
      },
    ]
    for (const { from, field, shift } of keyboardFocus) {
      row.querySelector<HTMLElement>(from)!.focus()
      await userEvent.tab({ shift })
      expect(document.activeElement).toBe(field)
      const focused = getComputedStyle(field)
      expect(focused.outlineStyle).toBe("solid")
      expect(focused.outlineWidth).toBe("2px")
    }
  })

  it("keeps property controls compact for a fine pointer", async () => {
    const wrapper = await mount()
    await expandPropertyPanel(wrapper, propertyRowSelector("url"))

    expect(matchMedia("(pointer: fine)").matches).toBe(true)
    const controls = interactiveControls(
      wrapper.get(propertyRowSelector("url")).element
    )
    expect(controls.length).toBeGreaterThan(3)
    for (const control of controls) {
      expect(rect(control).height).toBeLessThan(44)
    }
  })

  it("keeps a typed draft and its focus while the window narrows and widens", async () => {
    await atViewport(768)
    const wrapper = await mount()
    await (await startDraft(wrapper)).setValue("topic")
    const value = wrapper.get(`${DRAFT} ${VALUE_INPUT}`).element as HTMLElement
    value.focus()
    await userEvent.keyboard("a draft that must survive resizing")

    for (const width of [320, 1280]) {
      await atViewport(width)
      expect(document.activeElement).toBe(value)
      expect(value.textContent).toBe("a draft that must survive resizing")
      expectInside(rect(value), rect(wrapper.get(DRAFT).element))
    }
    await wrapper
      .get('[data-testid="rich-note-property-row-add"]')
      .trigger("click")
    await flushPromises()

    expect(h.lastEmittedMarkdown()).toContain(
      "topic: a draft that must survive resizing"
    )
  })

  it("keeps key presets clear of the draft value in a narrow editor at a wide viewport", async () => {
    await atViewport(1280)
    const wrapper = await mount()
    ;(wrapper.element as HTMLElement).style.width = "320px"
    const keyInput = await startDraft(wrapper)
    const presets = () => wrapper.get(PRESET_LIST).element

    expectApart(
      rect(presets()),
      rect(wrapper.get(`${DRAFT} ${VALUE_INPUT}`).element)
    )
    await keyInput.setValue("image")
    const choose = wrapper.get(
      '[data-testid="rich-note-image-property-choose"]'
    )
    expectApart(rect(presets()), rect(choose.element))
    expectApart(
      rect(presets()),
      rect(wrapper.get(`${DRAFT} ${VALUE_INPUT}`).element)
    )

    await wrapper
      .get(
        '[data-testid="rich-note-property-key-preset-option"][data-preset-key="image"]'
      )
      .trigger("click")
    await flushPromises()

    expect(wrapper.find(PRESET_LIST).exists()).toBe(false)
    expect(keyInput.element).toHaveProperty("value", "image")
    expectInside(rect(choose.element), rect(wrapper.get(DRAFT).element))
    expectInside(rect(wrapper.get(DRAFT).element), rect(wrapper.element))
  })

  it("shows a narrow draft's validation and accepts its correction", async () => {
    await atViewport(320)
    const wrapper = await mount()
    await (await startDraft(wrapper)).setValue("topic")
    const add = wrapper.get('[data-testid="rich-note-property-row-add"]')
    await add.trigger("click")
    const message = wrapper.get('[data-testid="rich-note-property-validation"]')

    expect(message.text()).toBe("Enter a property value.")
    expectInside(rect(message.element), rect(wrapper.element))
    await h.setPropertyValueField(
      wrapper.find(`${DRAFT} ${VALUE_INPUT}`),
      "training"
    )
    await add.trigger("click")
    await flushPromises()

    expect(h.lastEmittedMarkdown()).toContain("topic: training")
  })
})
