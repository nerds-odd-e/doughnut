import { advanceAnimationFrame } from "@tests/helpers/focusTargetTestSupport"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { propertyRowWithScalar } from "@/utils/noteContentPropertyRows"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const INSERT_KEY_INPUT = '[data-testid="rich-note-property-key"]'

describe("RichMarkdownEditor property entry", () => {
  const h = createRichMarkdownEditorTestHarness()

  beforeEach(() => {
    vi.useFakeTimers({ toFake: ["requestAnimationFrame"] })
  })

  afterEach(() => {
    vi.restoreAllMocks()
    h.cleanup()
    vi.useRealTimers()
  })

  describe("inserting a property", () => {
    it("Cancel drops the draft and its message without saving, reopening empty", async () => {
      const wrapper = await h.mountEditor("# Body")
      await h.openAddProperty()
      await wrapper.find(INSERT_KEY_INPUT).setValue("topic")
      await wrapper
        .find('[data-testid="rich-note-property-insert-add"]')
        .trigger("click")
      expect(
        wrapper.find('[data-testid="rich-note-property-validation"]').text()
      ).toBe("Enter a property value.")
      await h.setPropertyValueField(
        wrapper.find('[data-testid="rich-note-property-value"]'),
        "training"
      )

      await wrapper
        .find('[aria-label="Cancel adding property"]')
        .trigger("click")

      expect(wrapper.find(INSERT_KEY_INPUT).exists()).toBe(false)
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()
      expect(
        wrapper.find('[data-testid="rich-note-property-validation"]').exists()
      ).toBe(false)
      await h.openAddProperty()
      expect(wrapper.find(INSERT_KEY_INPUT).element).toHaveProperty("value", "")
      expect(
        wrapper.find('[data-testid="rich-note-property-value"]').text()
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
          '[data-testid="rich-note-property-value"]'
        )
        await keyInput.setValue(key)
        await h.setPropertyValueField(valueField, value)
        await wrapper
          .find('[data-testid="rich-note-property-insert-add"]')
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
      const add = wrapper.find('[data-testid="rich-note-property-insert-add"]')
      await add.trigger("click")
      expect(
        wrapper.find('[data-testid="rich-note-property-validation"]').text()
      ).toBe("Enter a property value.")

      await h.setPropertyValueField(
        wrapper.find('[data-testid="rich-note-property-value"]'),
        "training"
      )
      await add.trigger("click")

      expect(
        wrapper.find('[data-testid="rich-note-property-validation"]').exists()
      ).toBe(false)
      expect(h.lastEmittedMarkdown()).toContain("topic: training")
    })

    it("adds an image property from a typed URL", async () => {
      const wrapper = await h.mountEditor("# Hi")

      await h.openAddProperty()
      await wrapper
        .find('[data-testid="rich-note-property-key"]')
        .setValue("image")
      await flushPromises()

      const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
      await valInput.setValue("https://example.com/a.png")
      await wrapper
        .find('[data-testid="rich-note-property-insert-add"]')
        .trigger("click")

      expect(h.lastEmittedMarkdown()).toContain(
        "image: https://example.com/a.png"
      )
    })

    it("emits composed frontmatter and preserves body", async () => {
      await h.mountEditor("# Hello Body")
      await h.openAddProperty()
      await advanceAnimationFrame()

      const keyInput = h.getWrapper().find(INSERT_KEY_INPUT)
      const valInput = h
        .getWrapper()
        .find('[data-testid="rich-note-property-value"]')
      await keyInput.setValue("status")
      await h.setPropertyValueField(valInput, "draft")
      await h
        .getWrapper()
        .find('[data-testid="rich-note-property-insert-add"]')
        .trigger("click")

      const last = h.lastEmittedMarkdown()
      expect(last).toContain("---")
      expect(last).toContain("status: draft")
      expect(last).toContain("Hello Body")
    })

    it("Add saves exactly once and closes the draft", async () => {
      await h.mountEditor("# Body")
      await h.openAddProperty()
      const wrapper = h.getWrapper()
      await wrapper.find(INSERT_KEY_INPUT).setValue("status")
      await h.setPropertyValueField(
        wrapper.find('[data-testid="rich-note-property-value"]'),
        "draft"
      )
      await wrapper
        .find('[data-testid="rich-note-property-insert-add"]')
        .trigger("click")
      expect(wrapper.emitted("update:modelValue")).toHaveLength(1)
      expect(h.lastEmittedMarkdown()).toContain("status: draft")
      await wrapper.setProps({ modelValue: h.lastEmittedMarkdown() })
      expect(wrapper.find(INSERT_KEY_INPUT).exists()).toBe(false)
    })

    it.each(["status", "image"])(
      "Enter in the %s value field saves exactly once and closes the draft",
      async (key) => {
        await h.mountEditor("# Body", { attachToBody: true })
        await h.openAddProperty()
        const wrapper = h.getWrapper()
        await wrapper.find(INSERT_KEY_INPUT).setValue(key)
        const field = wrapper.find('[data-testid="rich-note-property-value"]')
        const value =
          key === "image" ? "https://example.com/image.png" : "draft"
        if (key === "image") {
          await field.setValue(value)
        } else {
          await h.setPropertyValueField(field, value)
        }
        ;(field.element as HTMLElement).focus()
        await field.trigger("keydown", { key: "Enter" })
        expect(wrapper.emitted("update:modelValue")).toHaveLength(1)
        expect(h.lastEmittedMarkdown()).toContain(`${key}: ${value}`)
        await wrapper.setProps({ modelValue: h.lastEmittedMarkdown() })
        expect(wrapper.find(INSERT_KEY_INPUT).exists()).toBe(false)
      }
    )

    it.each(["status", "image"])(
      "leaving the %s value keeps the draft without saving",
      async (key) => {
        await h.mountEditor("# Body")
        await h.openAddProperty()
        const wrapper = h.getWrapper()
        await wrapper.find(INSERT_KEY_INPUT).setValue(key)
        const field = wrapper.find('[data-testid="rich-note-property-value"]')
        if (key === "image") {
          await field.setValue("https://example.com/image.png")
        } else {
          await h.setPropertyValueField(field, "draft")
        }
        await field.trigger("blur")
        expect(wrapper.emitted("update:modelValue")).toBeUndefined()
        expect(wrapper.find(INSERT_KEY_INPUT).element).toHaveProperty(
          "value",
          key
        )
        if (key === "image") {
          expect(field.element).toHaveProperty(
            "value",
            "https://example.com/image.png"
          )
        } else {
          expect(field.text()).toBe("draft")
        }
      }
    )

    it("appends to exact list-capable keys without folding legacy suffixes", async () => {
      await h.mountEditor(
        `---
example of: "[[A]]"
example of 2: "[[B]]"
---

# Body`,
        { attachToBody: true }
      )

      await h.commitInsertProperty("example of", "[[C]]")
      const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
      expect(parsed.ok).toBe(true)
      if (!parsed.ok) return
      expect(parsed.properties["example of"]).toEqual(
        listPropertyValue(["[[A]]", "[[C]]"])
      )
      expect(parsed.properties["example of 2"]).toEqual(
        propertyRowWithScalar("example of 2", "[[B]]").value
      )
    })
  })
})
