import { flushPromises } from "@vue/test-utils"
import { AUTHORED_OVERLAPS_MESSAGE } from "@/utils/authoredOverlapsValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiTitleFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import {
  propertyRowListValue,
  propertyValidationText,
  triggerRowKeyBlurValidation,
} from "./propertiesTestDom"
import {
  clickListAdd,
  clickModeTab,
  dialogEl,
  mountPropertyValuePopup,
  popupValidationText,
  savePopup,
  setListItemValue,
  setTextareaValue,
} from "./propertyValuePopupTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const OVERLAPS_LIST_MARKDOWN = `---
overlaps:
  - "[[Other Note]]"
---

Body`

const OVERLAPS_SCALAR_MARKDOWN = `---
overlaps: "[[Other Note]]"
---

Body`

describe("RichMarkdownEditor overlaps property", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("rejects invalid overlaps in popup then saves a valid list", async () => {
    const wrapper = await mountPropertyValuePopup(h, OVERLAPS_LIST_MARKDOWN)

    clickModeTab("rich-note-property-value-popup-mode-text")
    await flushPromises()
    setTextareaValue("[[Other Note]]")
    await savePopup()
    expect(popupValidationText()).toBe(AUTHORED_OVERLAPS_MESSAGE)
    expect(dialogEl()).not.toBeNull()
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()

    clickModeTab("rich-note-property-value-popup-mode-list")
    await flushPromises()
    setListItemValue(0, "plain alias")
    await savePopup()
    expect(popupValidationText()).toBe(AUTHORED_OVERLAPS_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()

    setListItemValue(0, "[[Other Note]]")
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
    await h.mountAndCommitInsertProperty("overlaps", "[[Other Note]]")

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
    const emissionsBeforeBlur =
      wrapper.emitted("update:modelValue")?.length ?? 0
    await triggerRowKeyBlurValidation(wrapper)

    expect(propertyValidationText(wrapper.element)).toBe(
      AUTHORED_OVERLAPS_MESSAGE
    )
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

    const resolved = propertyRowListValue(wrapper, "overlaps")
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

    const livePath = propertyRowListValue(wrapper, "overlaps")
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

    const deadPath = propertyRowListValue(wrapper, "overlaps")
    const deadLink = deadPath.find("a.dead-wiki-link")
    expect(deadLink.exists()).toBe(true)
    expect(deadLink.text()).toBe("Title")
  })

  it("shows a new overlaps wiki link as pending until last-saved includes it", async () => {
    const inFlight = `---
overlaps:
  - "[[Other Note]]"
  - "[[WikiLinks E2E Nowhere]]"
---

Body`
    const wrapper = await h.mountEditor(inFlight, {
      lastSavedMarkdown: OVERLAPS_LIST_MARKDOWN,
      wikiTitles: [],
    })
    await flushPromises()

    const list = propertyRowListValue(wrapper, "overlaps")
    expect(list.find("a.pending-wiki-link").text()).toBe(
      "WikiLinks E2E Nowhere"
    )
    expect(list.find("a.dead-wiki-link").text()).toBe("Other Note")

    await wrapper.setProps({ lastSavedMarkdown: inFlight, wikiTitles: [] })
    await flushPromises()

    expect(list.find("a.pending-wiki-link").exists()).toBe(false)
    const deadTitles = list.findAll("a.dead-wiki-link").map((a) => a.text())
    expect(deadTitles).toEqual(["Other Note", "WikiLinks E2E Nowhere"])
  })
})
