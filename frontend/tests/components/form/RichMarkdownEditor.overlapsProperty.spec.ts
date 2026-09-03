import { flushPromises } from "@vue/test-utils"
import { AUTHORED_OVERLAPS_MESSAGE } from "@/utils/authoredOverlapsValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import {
  propertyRowListValue,
  propertyValidationText,
  triggerRowKeyBlurValidation,
} from "./propertiesTestDom"
import {
  propertyValueDialogEl,
  mountPropertyValueDialog,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
} from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const OVERLAPS_LIST_MARKDOWN = `---
overlaps:
  - "[[Other Note]]"
  - "[[Missing Note]]"
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

  it("rejects invalid overlaps in the property value dialog then saves a valid list", async () => {
    const wrapper = await mountPropertyValueDialog(h, OVERLAPS_LIST_MARKDOWN)

    setListItemValue(0, "plain alias")
    await savePropertyValueDialog()
    expect(propertyValueDialogValidationText()).toBe(AUTHORED_OVERLAPS_MESSAGE)
    expect(wrapper.emitted("update:modelValue")).toBeUndefined()

    setListItemValue(0, "[[Hue Note]]")
    await savePropertyValueDialog()

    const last = h.lastEmittedMarkdown()
    expect(last).toMatch(/overlaps:\s*\n\s*- ["']?\[\[Hue Note\]\]/)
    expect(propertyValueDialogEl()).toBeNull()
  })

  it("inserts overlaps as a list and blocks scalar overlaps on row commit", async () => {
    await h.mountEditor("# Body")
    await h.commitInsertProperty("overlaps", "[[Other Note]]")

    const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
    expect(parsed.ok).toBe(true)
    if (!parsed.ok) return
    expect(parsed.properties.overlaps).toEqual(
      listPropertyValue(["[[Other Note]]"])
    )

    const wrapper = h.getWrapper()
    await wrapper.setProps({ modelValue: OVERLAPS_SCALAR_MARKDOWN })
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

  it("renders resolved and dead overlaps, marking new links pending until saved", async () => {
    const inFlight = `---
overlaps:
  - "[[Other Note]]"
  - "[[Missing Note]]"
  - "[[WikiLinks E2E Nowhere]]"
---

Body`
    const wrapper = await h.mountEditor(inFlight, {
      lastSavedMarkdown: OVERLAPS_LIST_MARKDOWN,
      wikiLinks: [wikiLinkFromAuthoredToken("Other Note", 42)],
    })

    const list = propertyRowListValue(wrapper, "overlaps")
    const resolvedLink = list.find("a.router-link")
    expect(resolvedLink.text()).toBe("Other Note")
    expect(JSON.parse(resolvedLink.attributes("to") ?? "{}")).toEqual(
      noteShowLocation(42)
    )
    expect(list.text()).not.toContain("[[")
    expect(list.find("a.dead-wiki-link").text()).toBe("Missing Note")
    expect(list.find("a.pending-wiki-link").text()).toBe(
      "WikiLinks E2E Nowhere"
    )

    await wrapper.setProps({ lastSavedMarkdown: inFlight })
    await flushPromises()

    expect(list.find("a.pending-wiki-link").exists()).toBe(false)
    const deadTitles = list.findAll("a.dead-wiki-link").map((a) => a.text())
    expect(deadTitles).toEqual(["Missing Note", "WikiLinks E2E Nowhere"])
  })
})
