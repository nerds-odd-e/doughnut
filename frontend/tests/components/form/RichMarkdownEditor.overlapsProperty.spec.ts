import { flushPromises } from "@vue/test-utils"
import { AUTHORED_OVERLAPS_MESSAGE } from "@/utils/authoredOverlapsValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import {
  addNewOverlapsProperty,
  mountOverlapsValuePopup,
  OVERLAPS_LIST_MARKDOWN,
  OVERLAPS_SCALAR_MARKDOWN,
  overlapsListValue,
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

  it("inserts overlaps as a list and blocks scalar overlaps on row commit", async () => {
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

    const wrapper = h.getWrapper()
    await wrapper.setProps({ modelValue: OVERLAPS_SCALAR_MARKDOWN })
    await flushPromises()
    const emissionsBeforeBlur = wrapper.emitted("update:modelValue")?.length ?? 0
    await triggerRowKeyBlurValidation(wrapper)

    expect(propertyRowValidationText(wrapper)).toBe(AUTHORED_OVERLAPS_MESSAGE)
    expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
      emissionsBeforeBlur
    )
  })

  it("renders overlaps list items as wiki links (resolved, path live, path dead)", async () => {
    const pathItem = "[Title](/Folder/Title.md)"
    const wrapper = await h.mountEditor(OVERLAPS_LIST_MARKDOWN, {
      wikiTitles: [wikiTitleFromAuthoredToken("Other Note", 42)],
    })
    await flushPromises()

    const resolved = overlapsListValue(wrapper)
    const resolvedLink = resolved.find("a.router-link")
    expect(resolvedLink.exists()).toBe(true)
    expect(resolvedLink.text()).toBe("Other Note")
    expect(resolved.text()).not.toContain("[[")
    expect(JSON.parse(resolvedLink.attributes("to") ?? "{}")).toEqual(
      noteShowLocation(42)
    )

    await wrapper.setProps({
      modelValue: `---
overlaps:
  - "${pathItem}"
---

Body`,
      wikiTitles: [wikiTitleFromAuthoredToken(pathItem, 42)],
    })
    await flushPromises()

    const livePath = overlapsListValue(wrapper)
    const liveLink = livePath.find("a.router-link")
    expect(liveLink.exists()).toBe(true)
    expect(liveLink.text()).toBe("Title")
    expect(liveLink.attributes("data-wiki-title")).toBe("/Folder/Title.md")
    expect(JSON.parse(liveLink.attributes("to") ?? "{}")).toEqual(
      noteShowLocation(42)
    )
    expect(livePath.attributes("title")).toContain(pathItem)
    expect(livePath.text()).not.toContain("[[")

    await wrapper.setProps({
      modelValue: `---
overlaps:
  - "[Title](/Folder/Title.md)"
---

Body`,
      wikiTitles: [],
    })
    await flushPromises()

    const deadPath = overlapsListValue(wrapper)
    const deadLink = deadPath.find("a.dead-wiki-link")
    expect(deadLink.exists()).toBe(true)
    expect(deadLink.text()).toBe("Title")
  })
})
