import { flushPromises } from "@vue/test-utils"
import {
  attemptRenamePropertyKey,
  expandPropertyPanelAndClickRemove,
  propertyRowSelector,
  propertyValidationMessages,
  validationMessageAfterRow,
} from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property validation", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
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
