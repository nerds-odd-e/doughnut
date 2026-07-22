import { README_ONLY_PRESET_PROPERTY_KEYS } from "@/utils/noteContentFrontmatter"
import { flushPromises } from "@vue/test-utils"
import {
  commitCustomRelationText,
  customRelationRadioLabelEl,
  customRelationTextInputEl,
  openRelationDialog,
  relationTypeButtonText,
  selectCustomRelationRadio,
} from "./propertyRelationImageIndexTestDom"
import {
  editorRoot,
  emitQuillBodyHtml,
  mountRelationNote,
  propertyRowKeyValues,
} from "./propertyRelationImageIndexTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property relation and index", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  describe("relation property in rich mode", () => {
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
        await mountRelationNote(h, relation)
        const text = relationTypeButtonText(editorRoot(h))
        expect(text).toContain(expectedLabel)
        expect(text).not.toContain(notExpected)
      }
    )

    it("commits custom relationship text from the dialog and emits updated frontmatter", async () => {
      await mountRelationNote(h, "similar-to")
      await openRelationDialog(editorRoot(h))
      await selectCustomRelationRadio()
      await commitCustomRelationText("novel connector phrase")

      expect(h.lastEmittedMarkdown()).toContain(
        "relation: novel-connector-phrase"
      )
    })

    it("opens dialog with custom text prefilled for an unknown relation", async () => {
      await mountRelationNote(h, "xyz-unknown-kebab")
      await openRelationDialog(editorRoot(h))

      expect(document.querySelector("dialog")?.textContent).toContain("Custom…")
      expect(customRelationTextInputEl().value).toBe("xyz-unknown-kebab")
      expect(
        customRelationRadioLabelEl().classList.contains("bg-primary")
      ).toBe(true)
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

  describe("readme-only predefined properties", () => {
    it("does not show readme-only predefined rows when isReadmeContext is false", async () => {
      await h.mountEditor("# Body")
      await flushPromises()

      const keyValues = propertyRowKeyValues(editorRoot(h))
      for (const key of README_ONLY_PRESET_PROPERTY_KEYS) {
        expect(keyValues).not.toContain(key)
      }
    })

    it("empty readme-only fields are not included in emitted YAML", async () => {
      const wrapper = await h.mountEditor("# Body", { isReadmeContext: true })
      await emitQuillBodyHtml(wrapper, "<h1>Updated Body</h1>")

      const last = h.lastEmittedMarkdown()
      expect(last).not.toContain("title_pattern")
      expect(last).toContain("Updated Body")
    })

    it("readme-only fields are shown when note already has those keys in frontmatter", async () => {
      const markdown = `---
title_pattern: "{{date}}"
question_generation_instruction: Focus on facts.
---

# Body`
      const wrapper = await h.mountEditor(markdown, { isReadmeContext: true })
      await flushPromises()

      const keyValues = propertyRowKeyValues(editorRoot(h))
      expect(keyValues).toContain("title_pattern")
      expect(keyValues).toContain("question_generation_instruction")
      expect(wrapper.text()).toContain("{{date}}")
      expect(wrapper.text()).toContain("Focus on facts.")
    })
  })
})
