import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const INSERT_KEY_INPUT =
  '[data-property-draft="true"] [data-testid="rich-note-property-row-key-input"]'

describe("RichMarkdownEditor property draft lifecycle", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  it("Cancel drops the draft and its message without saving, reopening empty", async () => {
    const wrapper = await h.mountEditor("# Body")
    await h.openAddProperty()
    await wrapper.find(INSERT_KEY_INPUT).setValue("topic")
    await wrapper
      .find('[data-testid="rich-note-property-row-add"]')
      .trigger("click")
    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').text()
    ).toBe("Enter a property value.")
    await h.setPropertyValueField(
      wrapper.find(
        '[data-property-draft="true"] [data-testid="rich-note-property-row-value-input"]'
      ),
      "training"
    )

    await wrapper.find('[aria-label="Cancel adding property"]').trigger("click")

    expect(wrapper.find(INSERT_KEY_INPUT).exists()).toBe(false)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').exists()
    ).toBe(false)
    await h.openAddProperty()
    expect(wrapper.find(INSERT_KEY_INPUT).element).toHaveProperty("value", "")
    expect(
      wrapper
        .find(
          '[data-property-draft="true"] [data-testid="rich-note-property-row-value-input"]'
        )
        .text()
    ).toBe("")
  })

  it.each([
    { key: "topic", value: "", message: "Enter a property value." },
    { key: "", value: "training", message: "Enter a property key." },
    { key: "", value: "", message: "Enter a property key and value." },
  ])(
    "Add says $message and retains the draft without saving",
    async ({ key, value, message }) => {
      const wrapper = await h.mountEditor("# Body")
      await h.openAddProperty()
      const keyInput = wrapper.find(INSERT_KEY_INPUT)
      const valueField = wrapper.find(
        '[data-property-draft="true"] [data-testid="rich-note-property-row-value-input"]'
      )
      await keyInput.setValue(key)
      await h.setPropertyValueField(valueField, value)
      await wrapper
        .find('[data-testid="rich-note-property-row-add"]')
        .trigger("click")

      expect(
        wrapper.find('[data-testid="rich-note-property-validation"]').text()
      ).toBe(message)
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()
      expect(keyInput.element).toHaveProperty("value", key)
      expect(valueField.text()).toBe(value)
    }
  )

  it("clears the missing-field message on a successful Add", async () => {
    const wrapper = await h.mountEditor("# Body")
    await h.openAddProperty()
    await wrapper.find(INSERT_KEY_INPUT).setValue("topic")
    const add = wrapper.find('[data-testid="rich-note-property-row-add"]')
    await add.trigger("click")
    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').text()
    ).toBe("Enter a property value.")

    await h.setPropertyValueField(
      wrapper.find(
        '[data-property-draft="true"] [data-testid="rich-note-property-row-value-input"]'
      ),
      "training"
    )
    await add.trigger("click")

    expect(
      wrapper.find('[data-testid="rich-note-property-validation"]').exists()
    ).toBe(false)
    expect(h.lastEmittedMarkdown()).toContain("topic: training")
  })
})
