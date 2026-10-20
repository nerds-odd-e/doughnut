import { notePropertyLocation } from "@/routes/noteShowLocation"
import { flushPromises } from "@vue/test-utils"
import { propertyRowSelector } from "./propertiesTestDom"
import {
  expectInside,
  expectLegibleWholeValueInRow,
  expectWholeTextVisibleInRow,
  KEY_INPUT,
  mountAtWidth,
  propertyKeys,
  prose,
  rect,
  row,
  unbroken,
  VALUE_INPUT,
  widths,
} from "./propertyLayoutTestSupport"
import { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"

describe("RichMarkdownEditor property layout", () => {
  const h = createRichMarkdownEditorTestHarness()

  afterEach(() => {
    h.cleanup()
  })

  describe.each(widths)("at %ipx", (width) => {
    it("shows each whole editable property value and key within its row", async () => {
      const wrapper = await mountAtWidth(h, width)

      for (const key of propertyKeys) {
        const r = row(wrapper, key)
        expectLegibleWholeValueInRow(
          r,
          r.querySelector(VALUE_INPUT)!,
          wrapper.element
        )
        expectInside(rect(r.querySelector(KEY_INPUT)!), rect(r))
      }
    })

    it("shows each whole read-only key and value within its row", async () => {
      const wrapper = await mountAtWidth(h, width, { readonly: true })

      for (const key of propertyKeys) {
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
      const wrapper = await mountAtWidth(h, 320, { readonly })
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
    const wrapper = await mountAtWidth(h, 320)
    const r = row(wrapper, "identifier")
    const field = wrapper.find(
      `${propertyRowSelector("identifier")} ${VALUE_INPUT}`
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
