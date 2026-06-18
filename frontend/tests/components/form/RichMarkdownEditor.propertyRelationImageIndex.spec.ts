import { CUSTOM_RELATION_RADIO_SENTINEL } from "@/models/relationTypeOptions"
import { INDEX_ONLY_PRESET_PROPERTY_KEYS } from "@/utils/noteContentFrontmatter"
import { flushPromises } from "@vue/test-utils"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property relation and index", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  describe("relation property in rich mode", () => {
    async function mountRelation(relation: string) {
      await h.mountEditor(
        `---\nrelation: ${relation}\ntype: relationship\n---\n\nBody`
      )
      await flushPromises()
    }

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
    ])("relation button shows $expectedLabel for $relation", async ({
      relation,
      expectedLabel,
      notExpected,
    }) => {
      await mountRelation(relation)
      const btn = h.getWrapper().find('[aria-label="Relation Type"]')
      expect(btn.text()).toContain(expectedLabel)
      expect(btn.text()).not.toContain(notExpected)
    })

    it("opens relation dialog for unknown relation with Custom option", async () => {
      await mountRelation("my-custom-relation")
      await h.getWrapper().find('[aria-label="Relation Type"]').trigger("click")

      const rt = h.getWrapper().findComponent({ name: "RelationTypeSelect" })
      expect(rt.exists()).toBe(true)
      expect(rt.text()).toContain("Custom…")
    })

    it("commits custom relationship text from the dialog and emits updated frontmatter", async () => {
      await mountRelation("similar-to")
      await h.getWrapper().find('[aria-label="Relation Type"]').trigger("click")

      const rt = h.getWrapper().findComponent({ name: "RelationTypeSelect" })
      await rt
        .find(`input[value="${CUSTOM_RELATION_RADIO_SENTINEL}"]`)
        .trigger("change")

      const field = rt.find('input[type="text"].daisy-input')
      expect(field.exists()).toBe(true)
      await field.setValue("novel connector phrase")
      await field.trigger("keydown", { key: "Enter" })

      expect(h.lastEmittedMarkdown()).toContain(
        "relation: novel-connector-phrase"
      )
    })

    it("opens dialog with custom text prefilled for an unknown relation", async () => {
      await mountRelation("xyz-unknown-kebab")
      await h.getWrapper().find('[aria-label="Relation Type"]').trigger("click")

      const rt = h.getWrapper().findComponent({ name: "RelationTypeSelect" })
      const field = rt.find('input[type="text"].daisy-input')
      expect(field.exists()).toBe(true)
      expect((field.element as HTMLInputElement).value).toBe(
        "xyz-unknown-kebab"
      )
      const primaryLabelForCustom = rt.find(
        `label[for="rich-note-relation-property-${CUSTOM_RELATION_RADIO_SENTINEL}"]`
      )
      expect(primaryLabelForCustom.exists()).toBe(true)
      expect(primaryLabelForCustom.classes()).toContain("bg-primary")
    })
  })

  it("removing every property row emits body-only markdown and shows add-only chrome without Properties heading", async () => {
    const markdown = `---
only: x
---

Paragraph.\n`
    const wrapper = await h.mountEditor(markdown)

    expect(wrapper.text()).toContain("Properties")

    await wrapper
      .find('[data-testid="rich-note-property-row-remove"]')
      .trigger("click")

    const last = h.lastEmittedMarkdown()
    expect(last.startsWith("---")).toBe(false)
    expect(last).toContain("Paragraph.")

    await wrapper.setProps({ modelValue: last })

    expect(wrapper.find("h4").exists()).toBe(false)
    expect(wrapper.text()).not.toContain("Properties")
    expect(wrapper.text()).toContain("Add property")
  })

  describe("index-only predefined properties", () => {
    it("does not show index-only predefined rows when isIndexContext is false", async () => {
      const wrapper = await h.mountEditor("# Body")
      await flushPromises()

      const keyInputs = wrapper.findAll(
        '[data-testid="rich-note-property-row-key-input"]'
      )
      const keyValues = keyInputs.map(
        (el) => (el.element as HTMLInputElement).value
      )
      for (const key of INDEX_ONLY_PRESET_PROPERTY_KEYS) {
        expect(keyValues).not.toContain(key)
      }
    })

    it("empty index-only fields are not included in emitted YAML", async () => {
      const wrapper = await h.mountEditor("# Body", { isIndexContext: true })
      await flushPromises()

      const quill = wrapper.findComponent({ name: "QuillEditor" })
      quill.vm.$emit("update:modelValue", "<h1>Updated Body</h1>")
      await flushPromises()

      const last = h.lastEmittedMarkdown()
      expect(last).not.toContain("title_pattern")
      expect(last).toContain("Updated Body")
    })

    it("index-only fields are shown when note already has those keys in frontmatter", async () => {
      const markdown = `---
title_pattern: "{{date}}"
question_generation_instruction: Focus on facts.
---

# Body`
      const wrapper = await h.mountEditor(markdown, { isIndexContext: true })
      await flushPromises()

      const keyInputs = wrapper.findAll(
        '[data-testid="rich-note-property-row-key-input"]'
      )
      const keyValues = keyInputs.map(
        (el) => (el.element as HTMLInputElement).value
      )
      expect(keyValues).toContain("title_pattern")
      expect(keyValues).toContain("question_generation_instruction")
      expect(wrapper.text()).toContain("{{date}}")
      expect(wrapper.text()).toContain("Focus on facts.")
    })
  })
})
