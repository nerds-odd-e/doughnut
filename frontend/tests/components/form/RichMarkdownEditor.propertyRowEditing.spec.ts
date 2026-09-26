import { CUSTOM_RELATION_RADIO_SENTINEL } from "@/models/relationTypeOptions"
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
  propertyValidationText,
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

  it("adds an image property from a typed URL", async () => {
    const wrapper = await h.mountEditor("# Hi")

    await h.openAddProperty()
    await wrapper
      .find('[data-testid="rich-note-property-key"]')
      .setValue("image")
    await flushPromises()

    const valInput = wrapper.find('[data-testid="rich-note-property-value"]')
    await valInput.setValue("https://example.com/a.png")
    await valInput.trigger("blur")

    expect(h.lastEmittedMarkdown()).toContain(
      "image: https://example.com/a.png"
    )
  })

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

  describe("relation property in rich mode", () => {
    const mountRelationNote = (relation: string) =>
      h.mountEditor(
        `---\nrelation: ${relation}\ntype: Relationship\n---\n\nBody`
      )
    const relationTypeButton = () =>
      h.getWrapper().get('[aria-label="Relation Type"]')

    it.each([
      {
        relation: "similar-to",
        expectedLabel: "similar to",
        notExpected: "similar-to",
      },
      {
        relation: "my-custom-relation",
        expectedLabel: "my-custom-relation",
        notExpected: "related to",
      },
    ])(
      "relation button shows $expectedLabel for $relation",
      async ({ relation, expectedLabel, notExpected }) => {
        await mountRelationNote(relation)
        const text = relationTypeButton().text()
        expect(text).toContain(expectedLabel)
        expect(text).not.toContain(notExpected)
      }
    )

    it("opens custom relation dialog prefilled and commits updated frontmatter", async () => {
      await mountRelationNote("xyz-unknown-kebab")
      await relationTypeButton().trigger("click")
      await flushPromises()

      expect(document.querySelector("dialog")?.textContent).toContain("Custom…")
      const input = document.querySelector(
        "dialog input[type='text'].daisy-input"
      ) as HTMLInputElement
      expect(input.value).toBe("xyz-unknown-kebab")
      expect(
        document
          .querySelector(
            `label[for="rich-note-relation-property-${CUSTOM_RELATION_RADIO_SENTINEL}"]`
          )
          ?.classList.contains("bg-primary")
      ).toBe(true)

      input.value = "novel connector phrase"
      input.dispatchEvent(new Event("input", { bubbles: true }))
      input.dispatchEvent(
        new KeyboardEvent("keydown", { key: "Enter", bubbles: true })
      )
      await flushPromises()
      expect(h.lastEmittedMarkdown()).toContain(
        "relation: novel-connector-phrase"
      )
    })
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
})
