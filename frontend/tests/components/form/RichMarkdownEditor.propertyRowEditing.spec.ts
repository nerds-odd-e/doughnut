import { noteShowLocation } from "@/routes/noteShowLocation"
import { README_ONLY_PRESET_PROPERTY_KEYS } from "@/utils/noteContentFrontmatter"
import { flushPromises } from "@vue/test-utils"
import {
  attemptRenamePropertyKey,
  expandPropertyPanel,
  expandPropertyPanelAndClickRemove,
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowSelector,
  propertyValidationMessages,
  propertyValidationText,
  validationMessageAfterRow,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const twoPropertyMarkdown = `---
alpha: one
beta: two
---

Body line`

describe("RichMarkdownEditor property row editing", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  function propertyRowKeyValues(): string[] {
    return Array.from(
      h
        .getWrapper()
        .element.querySelectorAll(
          '[data-testid="rich-note-property-row-key-input"]'
        ),
      (el) => (el as HTMLInputElement).value
    )
  }

  it("updates an existing image property from typed text", async () => {
    const wrapper = await h.mountEditor(
      "---\nimage: https://example.com/old.png\n---\n\n# Hi"
    )

    const valInput = wrapper.find(
      '[data-testid="rich-note-property-row-value-input"]'
    )
    await valInput.setValue("https://example.com/new.png")
    await valInput.trigger("blur")

    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/new.png"
    )
  })

  it("rejects duplicate keys before emitting valid renamed keys and values", async () => {
    const wrapper = await h.mountEditor(twoPropertyMarkdown)
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await attemptRenamePropertyKey(wrapper, 1, "alpha")

    expect(propertyValidationText(wrapper.element)).toContain("Duplicate")
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(propertyRowKeyValues()[1]).toBe("beta")

    await attemptRenamePropertyKey(wrapper, 0, "domain")
    const domainValue = wrapper.find(
      `${propertyRowSelector("domain")} [data-testid="rich-note-property-row-value-input"]`
    )
    await h.setPropertyValueField(domainValue, "wiki")
    await domainValue.trigger("blur")

    const last = h.lastEmittedMarkdown()
    expect(last).toContain("domain:")
    expect(last).toContain("wiki")
    expect(last).not.toContain("alpha:")
  })

  it("opening one property panel then removing that row leaves the other collapsed", async () => {
    const wrapper = await h.mountEditor(twoPropertyMarkdown)
    const alphaRow = propertyRowSelector("alpha")
    const betaRow = propertyRowSelector("beta")

    await expandPropertyPanel(wrapper, alphaRow)

    expectPropertyPanelOpen(wrapper.find(alphaRow).element)
    expectPropertyPanelClosed(wrapper.find(betaRow).element)

    await wrapper
      .find(`${alphaRow} [data-testid="rich-note-property-row-remove"]`)
      .trigger("click")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("alpha:")
    expect(last).toContain("beta:")

    expectPropertyPanelClosed(wrapper.find(betaRow).element)
  })

  it("removing every property row emits body-only markdown and shows add-only chrome without Properties heading", async () => {
    const wrapper = await h.mountEditor("---\nonly: x\n---\n\nParagraph.\n", {
      route: noteShowLocation(42),
    })

    expect(wrapper.text()).toContain("Properties")

    await expandPropertyPanelAndClickRemove(
      wrapper,
      propertyRowSelector("only")
    )

    const last = h.lastEmittedMarkdown()
    expect(last.startsWith("---")).toBe(false)
    expect(last).toContain("Paragraph.")

    await wrapper.setProps({ modelValue: last })

    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add property")
  })

  it("keeps readme-only fields scoped to populated readme frontmatter", async () => {
    const wrapper = await h.mountEditor("# Body", { isReadmeContext: true })
    h.emitQuillModelValue("<h1>Updated Body</h1>")
    await flushPromises()

    const last = h.lastEmittedMarkdown()
    expect(last).not.toContain("title_pattern")
    expect(last).toContain("Updated Body")

    const markdown = `---
title_pattern: "{{date}}"
question_generation_instruction: Focus on facts.
---

# Body`
    await wrapper.setProps({ modelValue: markdown, isReadmeContext: true })

    expect(propertyRowKeyValues()).toContain("title_pattern")
    expect(propertyRowKeyValues()).toContain("question_generation_instruction")
    expect(wrapper.text()).toContain("{{date}}")
    expect(wrapper.text()).toContain("Focus on facts.")

    await wrapper.setProps({ modelValue: "# Body", isReadmeContext: false })

    const nonReadmeKeyValues = propertyRowKeyValues()
    for (const key of README_ONLY_PRESET_PROPERTY_KEYS) {
      expect(nonReadmeKeyValues).not.toContain(key)
    }
  })

  describe("rejected row change message", () => {
    const noteLevelMessage = "note_level must be an integer from 1 to 6."
    const twoRowMarkdown = "---\nnote_level: 3\nbeta: two\n---\n\nBody"

    async function editValue(
      wrapper: Awaited<ReturnType<typeof h.mountEditor>>,
      key: string,
      text: string
    ) {
      const field = wrapper.find(
        `${propertyRowSelector(key)} [data-testid="rich-note-property-row-value-input"]`
      )
      await field.trigger("focus")
      await h.setPropertyValueField(field, text)
      await field.trigger("blur")
      await flushPromises()
      return field
    }

    it("shows an invalid note_level directly under its row and restores the value", async () => {
      const wrapper = await h.mountEditor(twoRowMarkdown)

      const field = await editValue(wrapper, "note_level", "7")

      const message = validationMessageAfterRow(wrapper.element, "note_level")
      expect(message?.textContent?.trim()).toBe(noteLevelMessage)
      expect(field.element.textContent).toBe("3")
    })

    it("shows the duplicate-key message directly under the renamed row", async () => {
      const wrapper = await h.mountEditor(twoRowMarkdown)

      await attemptRenamePropertyKey(wrapper, 1, "note_level")

      const message = validationMessageAfterRow(wrapper.element, "beta")
      expect(message?.textContent).toContain("Duplicate")
    })

    it("moves the single message to the latest rejected row and clears it on accepted edit or removal", async () => {
      const wrapper = await h.mountEditor(twoRowMarkdown)

      await editValue(wrapper, "note_level", "7")
      await attemptRenamePropertyKey(wrapper, 1, "note_level")

      expect(propertyValidationMessages(wrapper.element)).toHaveLength(1)
      expect(
        validationMessageAfterRow(wrapper.element, "beta")?.textContent
      ).toContain("Duplicate")

      await editValue(wrapper, "note_level", "4")
      expect(propertyValidationMessages(wrapper.element)).toHaveLength(0)

      await attemptRenamePropertyKey(wrapper, 1, "note_level")
      expect(propertyValidationMessages(wrapper.element)).toHaveLength(1)
      await expandPropertyPanelAndClickRemove(
        wrapper,
        propertyRowSelector("beta")
      )
      expect(propertyValidationMessages(wrapper.element)).toHaveLength(0)
    })

    it("keeps the add form message above the form and outside any row", async () => {
      const wrapper = await h.mountEditor(twoRowMarkdown)

      await h.commitInsertProperty("note_level", "7")

      const messages = propertyValidationMessages(wrapper.element)
      expect(messages).toHaveLength(1)
      expect(
        messages[0]!.closest('[data-testid="rich-note-property-row"]')
      ).toBeNull()
      expect(
        messages[0]!.previousElementSibling?.matches(
          '[data-testid="rich-note-property-row"]'
        )
      ).toBe(false)
    })
  })
})
