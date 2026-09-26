import { flushPromises } from "@vue/test-utils"
import { nextTick } from "vue"
import {
  AUTHORED_ALIASES_MESSAGE,
  PIPE_ALIAS_WARNING,
} from "@/utils/authoredAliasesValidation"
import { AUTHORED_OVERLAPS_MESSAGE } from "@/utils/authoredOverlapsValidation"
import {
  listPropertyValue,
  parseNoteContentMarkdown,
} from "@/utils/noteContentFrontmatter"
import {
  propertyRowListValue,
  propertyRowSelector,
  propertyRows,
  propertyValidationText,
  triggerRowKeyBlurValidation,
} from "./propertiesTestDom"
import {
  mountPropertyValueDialog,
  PROPERTY_VALUE_DIALOG_OPEN_SELECTOR,
  propertyValueDialogEl,
  propertyValueDialogValidationText,
  savePropertyValueDialog,
  setListItemValue,
} from "./propertyValueDialogTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const listUrlMarkdown = `---
url:
  - https://example.com/a
  - https://example.com/b
---

# Body`

describe("RichMarkdownEditor list properties", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  it("shows imported list properties without parse error banner", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
example of:
  - one
  - two
---

# Body`
    const wrapper = await h.mountEditor(markdown)
    await flushPromises()

    expect(
      wrapper.find('[data-testid="rich-note-unavailable-warning"]').exists()
    ).toBe(false)
    expect(propertyRows(wrapper.element)).toHaveLength(2)
    expect(propertyRowListValue(wrapper, "tags").text()).toContain("alpha")
    expect(propertyRowListValue(wrapper, "example of").text()).toContain("one")
    expect(
      wrapper
        .find(
          `${propertyRowSelector("tags")} ${PROPERTY_VALUE_DIALOG_OPEN_SELECTOR}`
        )
        .exists()
    ).toBe(true)
  })

  it("shows per-item external links for list url in readonly mode", async () => {
    const wrapper = await h.mountEditor(listUrlMarkdown, { readonly: true })
    const links = wrapper
      .find("dl")
      .findAll('[data-testid="rich-note-property-external-link"]')
    expect(links.length).toBe(2)
    expect(wrapper.find("dl").text()).toContain("https://example.com/a")
  })

  it("shows per-item external links for list url in editable mode", async () => {
    const wrapper = await h.mountEditor(listUrlMarkdown)
    await flushPromises()

    expect(
      wrapper.findAll(
        `${propertyRowSelector("url")} [data-testid="rich-note-property-external-link"]`
      )
    ).toHaveLength(2)
  })

  it("shows list properties compactly in readonly mode", async () => {
    const markdown = `---
tags:
  - alpha
  - beta
---

# Body`
    const wrapper = await h.mountEditor(markdown, { readonly: true })
    expect(wrapper.find("dl").text()).toContain("alpha")
  })

  it.each([
    {
      key: "aliases",
      stored: "color",
      invalid: "bad#alias",
      message: AUTHORED_ALIASES_MESSAGE,
      valid: "hue",
      saved: /aliases:\s*\n\s*- hue/,
    },
    {
      key: "overlaps",
      stored: '"[[Other Note]]"',
      invalid: "plain alias",
      message: AUTHORED_OVERLAPS_MESSAGE,
      valid: "[[Hue Note]]",
      saved: /overlaps:\s*\n\s*- ["']?\[\[Hue Note\]\]/,
    },
  ])(
    "rejects an invalid $key item in the property value dialog then saves a valid list",
    async ({ key, stored, invalid, message, valid, saved }) => {
      const wrapper = await mountPropertyValueDialog(
        h,
        `---\n${key}:\n  - ${stored}\n---\n\nBody`
      )

      setListItemValue(0, invalid)
      await savePropertyValueDialog()
      expect(propertyValueDialogValidationText()).toBe(message)
      expect(wrapper.emitted("update:modelValue")).toBeUndefined()

      setListItemValue(0, valid)
      await savePropertyValueDialog()

      expect(h.lastEmittedMarkdown()).toMatch(saved)
      expect(propertyValueDialogEl()).toBeNull()
    }
  )

  it("saves a pipe alias with a link-compatibility warning", async () => {
    await mountPropertyValueDialog(h, "---\naliases:\n  - color\n---\n\nBody")

    setListItemValue(0, "A|B")
    await nextTick()
    expect(
      document.querySelector(
        '[data-testid="rich-note-property-value-dialog-warning"]'
      )?.textContent
    ).toBe(PIPE_ALIAS_WARNING)
    expect(PIPE_ALIAS_WARNING).not.toContain("Windows")
    await savePropertyValueDialog()

    expect(h.lastEmittedMarkdown()).toMatch(/aliases:\s*\n\s*- A\|B/)
    expect(propertyValueDialogEl()).toBeNull()
  })

  it.each([
    { key: "aliases", value: "color", message: AUTHORED_ALIASES_MESSAGE },
    {
      key: "overlaps",
      value: "[[Other Note]]",
      message: AUTHORED_OVERLAPS_MESSAGE,
    },
  ])(
    "inserts $key as a list and blocks a scalar $key on row commit",
    async ({ key, value, message }) => {
      await h.mountEditor("# Body")
      await h.commitInsertProperty(key, value)

      const parsed = parseNoteContentMarkdown(h.lastEmittedMarkdown())
      expect(parsed.ok).toBe(true)
      if (!parsed.ok) return
      expect(parsed.properties[key]).toEqual(listPropertyValue([value]))

      const wrapper = h.getWrapper()
      await wrapper.setProps({
        modelValue: `---\n${key}: "${value}"\n---\n\nBody`,
      })
      const emissionsBeforeBlur =
        wrapper.emitted("update:modelValue")?.length ?? 0
      await triggerRowKeyBlurValidation(wrapper)

      expect(propertyValidationText(wrapper.element)).toBe(message)
      expect(wrapper.emitted("update:modelValue")?.length ?? 0).toBe(
        emissionsBeforeBlur
      )
    }
  )
})
