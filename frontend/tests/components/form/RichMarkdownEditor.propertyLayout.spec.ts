import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { propertyRowSelector } from "./propertiesTestDom"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

const longKey = "a_remarkably_long_property_key_for_narrow_windows"
const prose =
  "A complete sentence of authored prose that is much longer than any narrow window and must wrap across several lines so the reader can see every word of it, even in a wide window where a single unwrapped line would still run far past the right edge of the row."
const unbroken = `Pneumono${"ultramicroscopicsilicovolcano".repeat(4)}coniosis`
const url =
  "https://example.com/a/really/long/path/that/keeps/going/and/going/with?query=parameters&and=more-parameters"
const linkToken =
  "Moon#prop:a%20part%20of|a long labelled link to the part-of property of the Moon note"

const markdown = `---
${longKey}: ${prose}
identifier: ${unbroken}
url: ${url}
topic: "[[${linkToken}]]"
---

Body`

const scalarKeys = [longKey, "identifier", "url", "topic"]
const widths = [320, 768, 1280]

function rect(el: Element) {
  return el.getBoundingClientRect()
}

function contentRect(el: Element) {
  const range = document.createRange()
  range.selectNodeContents(el)
  return range.getBoundingClientRect()
}

function expectInside(inner: DOMRect, outer: DOMRect) {
  expect(inner.left).toBeGreaterThanOrEqual(outer.left - 0.5)
  expect(inner.right).toBeLessThanOrEqual(outer.right + 0.5)
}

function expectWholeTextVisibleInRow(
  row: Element,
  text: Element,
  editor: Element
) {
  expect(text.scrollWidth).toBeLessThanOrEqual(text.clientWidth + 1)
  expectInside(contentRect(text), rect(row))
  expectInside(rect(row), rect(editor))
}

function expectLegibleWholeValueInRow(
  row: Element,
  value: Element,
  editor: Element
) {
  expectWholeTextVisibleInRow(row, value, editor)
  expect(rect(value).width).toBeGreaterThanOrEqual(rect(row).width / 2)
}

describe("RichMarkdownEditor property layout", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  async function mountAtWidth(
    width: number,
    options: Record<string, unknown> = {}
  ) {
    const wrapper = await h.mountEditor(markdown, {
      attachToBody: true,
      lastSavedMarkdown: markdown,
      wikiLinks: [wikiLinkFromAuthoredToken(linkToken, 42)],
      route: noteShowLocation(99),
      noteId: 99,
      ...options,
    })
    ;(wrapper.element as HTMLElement).style.width = `${width}px`
    await flushPromises()
    return wrapper
  }

  function row(wrapper: VueWrapper, key: string) {
    return wrapper.get(propertyRowSelector(key)).element
  }

  describe.each(widths)("at %ipx", (width) => {
    it("shows each whole editable scalar value and key within its row", async () => {
      const wrapper = await mountAtWidth(width)

      for (const key of scalarKeys) {
        const r = row(wrapper, key)
        expectLegibleWholeValueInRow(
          r,
          r.querySelector(
            '[data-testid="rich-note-property-row-value-input"]'
          )!,
          wrapper.element
        )
        expectInside(
          rect(
            r.querySelector('[data-testid="rich-note-property-row-key-input"]')!
          ),
          rect(r)
        )
      }
    })

    it("shows each whole read-only key and value within its row", async () => {
      const wrapper = await mountAtWidth(width, { readonly: true })

      for (const key of scalarKeys) {
        const r = row(wrapper, key)
        expectLegibleWholeValueInRow(r, r.querySelector("dd")!, wrapper.element)
        expectWholeTextVisibleInRow(r, r.querySelector("dt")!, wrapper.element)
      }
    })
  })

  it.each([
    { mode: "editable", readonly: false, link: "a.donut-wiki-link" },
    { mode: "read-only", readonly: true, link: "dd a" },
  ])(
    "follows the long labelled $mode link to its property in a narrow window",
    async ({ readonly, link }) => {
      const wrapper = await mountAtWidth(320, { readonly })
      const anchor = row(wrapper, "topic").querySelector(link) as HTMLElement

      expect(anchor.textContent).toContain("a long labelled link")
      anchor.click()
      await flushPromises()

      expect(wrapper.vm.$router.currentRoute.value).toMatchObject(
        notePropertyLocation(42, "a part of")
      )
    }
  )

  it("edits a long scalar inline in a narrow window and commits it on Enter", async () => {
    const wrapper = await mountAtWidth(320)
    const r = row(wrapper, "identifier")
    const field = wrapper.find(
      `${propertyRowSelector("identifier")} [data-testid="rich-note-property-row-value-input"]`
    )
    const edited = `${prose} ${unbroken}`

    ;(field.element as HTMLElement).focus()
    await h.setPropertyValueField(field, edited)
    expectLegibleWholeValueInRow(r, field.element, wrapper.element)
    await field.trigger("keydown", { key: "Enter" })
    await flushPromises()

    expect(h.lastEmittedMarkdown()).toContain(`identifier: ${edited}`)
  })
})
