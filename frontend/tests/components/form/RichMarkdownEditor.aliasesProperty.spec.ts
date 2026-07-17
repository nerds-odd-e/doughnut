import { flushPromises } from "@vue/test-utils"
import { AUTHORED_ALIASES_MESSAGE } from "@/utils/authoredAliasesValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import {
  addNewAliasesProperty,
  ALIASES_SCALAR_MARKDOWN,
  mountAliasesValuePopup,
  POPUP_ALIAS_CONSTRAINT_CASES,
  propertyRowValidationText,
  triggerRowKeyBlurValidation,
} from "./aliasesPropertyTestSupport"
import {
  clickListAdd,
  dialogEl,
  popupValidationText,
  savePopup,
  setListItemValue,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor aliases property", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it.each(POPUP_ALIAS_CONSTRAINT_CASES)(
    "shows alias constraint for $case",
    async ({ prepareInvalidValue, expectDialogOpen }) => {
      const wrapper = await mountAliasesValuePopup(h)
      await prepareInvalidValue()
      await savePopup()

      expect(popupValidationText()).toBe(AUTHORED_ALIASES_MESSAGE)
      if (expectDialogOpen) {
        expect(dialogEl()).not.toBeNull()
      }
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()
    }
  )

  it("emits valid aliases list edits from popup", async () => {
    await mountAliasesValuePopup(h)
    clickListAdd()
    await flushPromises()
    setListItemValue(1, "hue")
    await savePopup()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/aliases:\s*\n\s*- color/)
    expect(last).toMatch(/- hue/)
    expect(dialogEl()).toBeNull()
  })

  it("inserts the first alias as a list when adding a new aliases property", async () => {
    await addNewAliasesProperty(h, "color")

    expect(
      h
        .getWrapper()
        .find('[data-testid="rich-note-property-validation"]')
        .exists()
    ).toBe(false)
    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.aliases).toEqual(listPropertyValue(["color"]))
  })

  it("blocks commit when parsed aliases row is scalar", async () => {
    const wrapper = await h.mountEditor(ALIASES_SCALAR_MARKDOWN)
    await triggerRowKeyBlurValidation(wrapper)

    expect(propertyRowValidationText(wrapper)).toBe(AUTHORED_ALIASES_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })
})
