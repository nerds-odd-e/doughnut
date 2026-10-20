import { screen } from "@testing-library/vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, onTestFinished } from "vitest"
import { page } from "vitest/browser"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
  document.documentElement.removeAttribute("data-theme")
})

async function viewport(width: number) {
  const original = { width: window.innerWidth, height: window.innerHeight }
  onTestFinished(() => page.viewport(original.width, original.height))
  await page.viewport(width, 900)
}

it.each([375, 1440])(
  "keeps a readable bounded body and shared title/details alignment at %spx",
  async (width) => {
    await viewport(width)
    wrapper = mountNoteTextContent(
      makeMe.aNote
        .title("Reading a note")
        .content(
          `A paragraph with room to read. ${"The next thought follows naturally. ".repeat(80)}\n\n${"unbroken".repeat(100)}`
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
        .get('[data-testid="note-details-toggle"]')
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

it("keeps a full date and unfinished-edit cue on one phone-width line beside truncated keys", async () => {
  await viewport(375)
  wrapper = mountNoteTextContent(
    makeMe.aNote
      .content(`---\n${"long_authored_key_".repeat(10)}: value\n---\nBody.`)
      .updatedAt("2026-09-30T12:00:00Z")
      .please()
  )
  await flushPromises()
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  screen.getByText("Add property").click()
  await flushPromises()
  const key = screen.getByLabelText("Property key") as HTMLInputElement
  key.value = "draft"
  key.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")

  const toggle = screen.getByTestId("note-details-toggle")
  const summary = screen.getByTestId("note-property-summary")
  const date = screen.getByTestId("note-updated-at")
  const cue = screen.getByTestId("note-details-unfinished-edit")
  const line = toggle.getBoundingClientRect()
  expect(summary.scrollWidth).toBeGreaterThan(summary.clientWidth)
  expect(summary.clientWidth).toBeGreaterThan(0)
  expect(date.textContent).toContain("September 30, 2026")
  expect(cue.checkVisibility()).toBe(true)
  for (const element of [summary, date, cue]) {
    const bounds = element.getBoundingClientRect()
    expect(bounds.left).toBeGreaterThanOrEqual(line.left)
    expect(bounds.right).toBeLessThanOrEqual(line.right)
    expect(bounds.top).toBeCloseTo(date.getBoundingClientRect().top, 0)
  }
  expect(toggle.scrollWidth).toBeLessThanOrEqual(toggle.clientWidth)
  expect(date.scrollWidth).toBeLessThanOrEqual(date.clientWidth)
})

function renderedColor(element: Element) {
  const canvas = document.createElement("canvas")
  canvas.width = canvas.height = 1
  const context = canvas.getContext("2d")!
  context.fillStyle = getComputedStyle(element).color
  context.fillRect(0, 0, 1, 1)
  return [...context.getImageData(0, 0, 1, 1).data]
}

it.each(["light", "dark"])(
  "keeps details quieter than body text in the %s theme",
  async (theme) => {
    document.documentElement.setAttribute("data-theme", theme)
    wrapper = mountNoteTextContent(makeMe.aNote.content("Body.").please())
    await flushPromises()
    const body = renderedColor(wrapper.get(".ql-editor p").element)
    const details = renderedColor(screen.getByTestId("note-details-toggle"))
    expect(details[3]).toBeGreaterThan(100)
    expect(details[3]).toBeLessThan(body[3]!)
    for (const channel of [0, 1, 2]) {
      expect(Math.abs(body[channel]! - details[channel]!)).toBeLessThanOrEqual(
        2
      )
    }
  }
)
