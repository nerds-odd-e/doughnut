import { noteShowLocation } from "@/routes/noteShowLocation"
import { wikiLinkFromAuthoredToken } from "@/utils/wikiLinkMarkup"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { propertyRowSelector } from "./propertiesTestDom"
import type { createRichMarkdownEditorTestHarness } from "./richMarkdownEditorTestHarness"
import { page, server } from "vitest/browser"

export const longKey = "a_remarkably_long_property_key_for_narrow_windows"
export const prose =
  "A complete sentence of authored prose that is much longer than any narrow window and must wrap across several lines so the reader can see every word of it, even in a wide window where a single unwrapped line would still run far past the right edge of the row."
export const unbroken = `Pneumono${"ultramicroscopicsilicovolcano".repeat(4)}coniosis`
export const url =
  "https://example.com/a/really/long/path/that/keeps/going/and/going/with?query=parameters&and=more-parameters"
const linkToken =
  "Moon#prop:a%20part%20of|a long labelled link to the part-of property of the Moon note"

export const listKey = "url 2"
export const listItemLabels = [
  "a long labelled link to the part-of property of the Moon note",
  url,
  "practice",
  "practice",
  "",
  unbroken,
]

const markdown = `---
${longKey}: ${prose}
identifier: ${unbroken}
url: ${url}
topic: "[[${linkToken}]]"
${listKey}:
  - "[[${linkToken}]]"
  - "${url}"
  - "practice"
  - "practice"
  - ""
  - "${unbroken}"
---

Body`

export const propertyKeys = [longKey, "identifier", "url", "topic", listKey]
export const widths = [320, 768, 1280]

export async function mountAtWidth(
  h: ReturnType<typeof createRichMarkdownEditorTestHarness>,
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

export function row(wrapper: VueWrapper, key: string) {
  return wrapper.get(propertyRowSelector(key)).element
}

export function rect(el: Element) {
  return el.getBoundingClientRect()
}

export function contentRect(el: Element) {
  const range = document.createRange()
  range.selectNodeContents(el)
  return range.getBoundingClientRect()
}

export function expectInside(inner: DOMRect, outer: DOMRect) {
  expect(inner.left).toBeGreaterThanOrEqual(outer.left - 0.5)
  expect(inner.right).toBeLessThanOrEqual(outer.right + 0.5)
}

export function expectApart(a: DOMRect, b: DOMRect) {
  const apart =
    a.right <= b.left + 0.5 ||
    b.right <= a.left + 0.5 ||
    a.bottom <= b.top + 0.5 ||
    b.bottom <= a.top + 0.5
  expect(apart).toBe(true)
}

export function expectWholeTextVisibleInRow(
  row: Element,
  text: Element,
  editor: Element
) {
  expect(text.scrollWidth).toBeLessThanOrEqual(text.clientWidth + 1)
  expectInside(contentRect(text), rect(row))
  expectInside(rect(row), rect(editor))
}

export function expectLegibleWholeValueInRow(
  row: Element,
  value: Element,
  editor: Element
) {
  expectWholeTextVisibleInRow(row, value, editor)
  expect(rect(value).width).toBeGreaterThanOrEqual(rect(row).width / 2)
}

export const DRAFT = '[data-property-draft="true"]'
export const KEY_INPUT = '[data-testid="rich-note-property-row-key-input"]'
export const VALUE_INPUT = '[data-testid="rich-note-property-row-value-input"]'
export const PRESET_LIST = '[data-testid="rich-note-property-key-preset-list"]'

export async function atViewport(width: number, height = 800) {
  const original = server.config.browser.viewport
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, height)
}

export function interactiveControls(root: Element): HTMLElement[] {
  return Array.from(
    root.querySelectorAll<HTMLElement>(
      'button, input:not([type="file"]), [role="textbox"]'
    )
  )
}
