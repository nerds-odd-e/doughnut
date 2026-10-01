import { noteShowLocation } from "@/routes/noteShowLocation"
import { README_ONLY_PRESET_PROPERTY_KEYS } from "@/utils/noteContentFrontmatter"
import { flushPromises } from "@vue/test-utils"
import {
  attemptRenamePropertyKey,
  expectPresetOptions,
  expandPropertyPanel,
  expandPropertyPanelAndClickRemove,
  expectPropertyPanelClosed,
  expectPropertyPanelOpen,
  propertyRowSelector,
  propertyInputEl,
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

  it("omits another row's list preset while retaining the current row's own preset", async () => {
    await h.mountEditor(
      "---\ntopic: grammar\nurl: https://example.com\n---\n\nBody",
      {
        attachToBody: true,
      }
    )
    propertyInputEl(
      `${propertyRowSelector("topic")} [data-testid="rich-note-property-row-key-input"]`
    ).focus()
    await flushPromises()
    expectPresetOptions([
      "aliases",
      "overlaps",
      "note_level",
      "image",
      "wikidata_id",
      "example of",
      "question_generation_instruction",
    ])

    propertyInputEl(
      `${propertyRowSelector("url")} [data-testid="rich-note-property-row-key-input"]`
    ).focus()
    await flushPromises()
    expectPresetOptions([
      "aliases",
      "overlaps",
      "note_level",
      "image",
      "wikidata_id",
      "url",
      "example of",
      "question_generation_instruction",
    ])
  })

  it("rejects manually renaming topic to an occupied list key without emitting a save", async () => {
    const wrapper = await h.mountEditor(
      "---\ntopic: grammar\nurl: [https://one.example, https://two.example]\n---\n\nBody"
    )
    const emitCountBefore = wrapper.emitted("update:modelValue")?.length ?? 0

    await attemptRenamePropertyKey(wrapper, 0, "url")

    expect(
      validationMessageAfterRow(wrapper.element, "topic")?.textContent
    ).toContain("Duplicate")
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emitCountBefore
    )
    expect(propertyRowKeyValues()).toEqual(["topic", "url"])
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
})
