import { flushPromises } from "@vue/test-utils"
import { AUTHORED_OVERLAPS_MESSAGE } from "@/utils/authoredOverlapsValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiTitleFromInnerAndNoteId } from "@/utils/wikiLinkMarkup"
import {
  addNewOverlapsProperty,
  mountOverlapsValuePopup,
  OVERLAPS_LIST_MARKDOWN,
  OVERLAPS_SCALAR_MARKDOWN,
  POPUP_OVERLAPS_CONSTRAINT_CASES,
  propertyRowValidationText,
  triggerRowKeyBlurValidation,
} from "./overlapsPropertyTestSupport"
import {
  clickListAdd,
  dialogEl,
  popupValidationText,
  savePopup,
  setListItemValue,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor overlaps property", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it.each(POPUP_OVERLAPS_CONSTRAINT_CASES)(
    "shows overlaps constraint for $case",
    async ({ prepareInvalidValue, expectDialogOpen }) => {
      const wrapper = await mountOverlapsValuePopup(h)
      await prepareInvalidValue()
      await savePopup()

      expect(popupValidationText()).toBe(AUTHORED_OVERLAPS_MESSAGE)
      if (expectDialogOpen) {
        expect(dialogEl()).not.toBeNull()
      }
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()
    }
  )

  it("emits valid overlaps list edits from popup", async () => {
    await mountOverlapsValuePopup(h)
    clickListAdd()
    await flushPromises()
    setListItemValue(1, "[[Hue Note]]")
    await savePopup()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/overlaps:\s*\n\s*- ["']?\[\[Other Note\]\]/)
    expect(last).toMatch(/\[\[Hue Note\]\]/)
    expect(dialogEl()).toBeNull()
  })

  it("inserts the first overlap as a list when adding a new overlaps property", async () => {
    await addNewOverlapsProperty(h, "[[Other Note]]")

    expect(
      h
        .getWrapper()
        .find('[data-testid="rich-note-property-validation"]')
        .exists()
    ).toBe(false)
    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.overlaps).toEqual(
      listPropertyValue(["[[Other Note]]"])
    )
  })

  it("blocks commit when parsed overlaps row is scalar", async () => {
    const wrapper = await h.mountEditor(OVERLAPS_SCALAR_MARKDOWN)
    await triggerRowKeyBlurValidation(wrapper)

    expect(propertyRowValidationText(wrapper)).toBe(AUTHORED_OVERLAPS_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()
  })

  it("renders overlaps list items as in-app wiki links", async () => {
    const wrapper = await h.mountEditor(OVERLAPS_LIST_MARKDOWN, {
      wikiTitles: [wikiTitleFromInnerAndNoteId("Other Note", 42)],
    })
    await flushPromises()

    const overlapsRow = wrapper
      .findAll('[data-testid="rich-note-property-row"]')
      .find(
        (r) => (r.element as HTMLElement).dataset.propertyKey === "overlaps"
      )
    expect(overlapsRow).toBeDefined()
    const listValue = overlapsRow!.find(
      '[data-testid="rich-note-property-row-list-value"]'
    )
    const link = listValue.find("a.router-link")
    expect(link.exists()).toBe(true)
    expect(link.text()).toBe("Other Note")
    expect(listValue.text()).not.toContain("[[")
    expect(JSON.parse(link.attributes("to") ?? "{}")).toEqual(
      noteShowLocation(42)
    )
  })
})
