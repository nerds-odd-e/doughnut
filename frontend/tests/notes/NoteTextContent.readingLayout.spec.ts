import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, onTestFinished } from "vitest"
import { page } from "vitest/browser"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
})

async function viewport(width: number) {
  const original = { width: window.innerWidth, height: window.innerHeight }
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, 900)
}

it.each([375, 1440])(
  "keeps a readable bounded body and shared title/property alignment at %spx",
  async (width) => {
    await viewport(width)
    wrapper = mountNoteTextContent(
      makeMe.aNote
        .title("Reading a note")
        .content(
          `---\ntopic: reading\n---\n\nA paragraph with room to read. ${"The next thought follows naturally. ".repeat(80)}\n\n${"unbroken".repeat(100)}`
        )
        .please(),
      { readonly: true }
    )
    await flushPromises()

    const surface = wrapper.element as HTMLElement
    const editor = wrapper.get(".ql-editor").element as HTMLElement
    const bounds = surface.getBoundingClientRect()
    const bodyBounds = editor.getBoundingClientRect()
    expect(bounds.width).toBeLessThanOrEqual(768)
    expect(bodyBounds.width).toBeLessThanOrEqual(736)
    expect(bodyBounds.left).toBeGreaterThanOrEqual(16)
    expect(width - bodyBounds.right).toBeGreaterThanOrEqual(16)
    expect(surface.scrollWidth).toBeLessThanOrEqual(surface.clientWidth)
    expect(editor.scrollWidth).toBeLessThanOrEqual(editor.clientWidth)
    expect(
      wrapper.get('[data-test="note-title"]').element.getBoundingClientRect()
        .left
    ).toBeCloseTo(bodyBounds.left, 0)
    expect(
      wrapper
        .get('[data-testid="rich-note-property-row"]')
        .element.getBoundingClientRect().left
    ).toBeCloseTo(bodyBounds.left, 0)

    const typography = getComputedStyle(editor)
    const fontSize = Number.parseFloat(typography.fontSize)
    expect(fontSize).toBeCloseTo(17)
    expect(
      Number.parseFloat(typography.lineHeight) / fontSize
    ).toBeGreaterThanOrEqual(1.5)
    expect(
      Number.parseFloat(typography.lineHeight) / fontSize
    ).toBeLessThanOrEqual(1.6)
    const text = document.createRange()
    text.selectNodeContents(editor)
    for (const line of text.getClientRects()) {
      expect(line.left).toBeGreaterThanOrEqual(bodyBounds.left - 1)
      expect(line.right).toBeLessThanOrEqual(bodyBounds.right + 1)
    }
  }
)
