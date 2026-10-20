import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it, vi } from "vitest"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
  document.documentElement.removeAttribute("data-theme")
  vi.restoreAllMocks()
})

it("shows unambiguous dates after the property rows and before the body when revealed", async () => {
  const note = makeMe.aNote
    .content("---\ntopic: training\n---\n\nBody.")
    .createdAt("2026-02-10T12:00:00Z")
    .updatedAt("2026-05-15T12:00:00Z")
    .please()
  wrapper = mountNoteTextContent(note)
  await flushPromises()

  expect(wrapper.get('[data-testid="note-updated-at"]').text()).toBe(
    "Updated: May 15, 2026"
  )
  const dates = wrapper.get('[data-testid="note-detail-dates"]')
  expect((dates.element as HTMLElement).checkVisibility()).toBe(false)
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  expect((dates.element as HTMLElement).checkVisibility()).toBe(true)
  expect(dates.findAll("time").map((date) => date.text())).toEqual([
    "February 10, 2026",
    "May 15, 2026",
  ])
  const properties = wrapper.get("section").element.getBoundingClientRect()
  const dateBounds = dates.element.getBoundingClientRect()
  const body = wrapper.get(".ql-container").element.getBoundingClientRect()
  expect(dateBounds.top).toBeGreaterThanOrEqual(properties.bottom)
  expect(dateBounds.bottom).toBeLessThanOrEqual(body.top)
})

it("keeps dates available before the raw frontmatter textarea in Markdown mode", async () => {
  wrapper = mountNoteTextContent(
    makeMe.aNote
      .content("---\ntopic: training\n---\n\nBody.")
      .createdAt("2026-02-10T12:00:00Z")
      .updatedAt("2026-05-15T12:00:00Z")
      .please()
  )
  await wrapper.setProps({ asMarkdown: true })
  await flushPromises()
  expect(wrapper.get('[data-testid="note-updated-at"]').text()).toBe(
    "Updated: May 15, 2026"
  )
  const textarea = wrapper.get("textarea").element as HTMLTextAreaElement
  expect(textarea.checkVisibility()).toBe(true)
  expect(textarea.value).toBe("---\ntopic: training\n---\n\nBody.")
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  const dates = wrapper.get('[data-testid="note-detail-dates"]')
    .element as HTMLElement
  expect(dates.checkVisibility()).toBe(true)
  expect(dates.getBoundingClientRect().bottom).toBeLessThanOrEqual(
    textarea.getBoundingClientRect().top
  )
})

it("shows just the updated date on a read-only note without properties", async () => {
  wrapper = mountNoteTextContent(
    makeMe.aNote
      .content("Body.")
      .createdAt("2026-02-10T12:00:00Z")
      .updatedAt("2026-05-15T12:00:00Z")
      .please(),
    { readonly: true }
  )
  await flushPromises()
  const toggle = wrapper.get('[data-testid="note-details-toggle"]')
  expect(toggle.text()).toBe("Updated: May 15, 2026")
  await toggle.trigger("click")
  const dates = wrapper.get('[data-testid="note-detail-dates"]')
  expect((dates.element as HTMLElement).checkVisibility()).toBe(true)
  expect(dates.text()).toContain("February 10, 2026")
})

it.each(["light", "dark"])(
  "gives a recent updated date more emphasis in the %s theme",
  async (theme) => {
    document.documentElement.setAttribute("data-theme", theme)
    const now = new Date("2026-05-15T12:00:00Z").getTime()
    vi.spyOn(Date, "now").mockReturnValue(now)
    wrapper = mountNoteTextContent(
      makeMe.aNote
        .updatedAt(new Date(now - 60 * 60 * 1000).toISOString())
        .please()
    )
    await flushPromises()
    const recent = Number(
      getComputedStyle(wrapper.get('[data-testid="note-updated-at"]').element)
        .opacity
    )
    await wrapper.setProps({
      note: makeMe.aNote.updatedAt("2025-05-15T12:00:00Z").please(),
    })
    const old = Number(
      getComputedStyle(wrapper.get('[data-testid="note-updated-at"]').element)
        .opacity
    )
    expect(recent).toBeGreaterThan(old)
  }
)
