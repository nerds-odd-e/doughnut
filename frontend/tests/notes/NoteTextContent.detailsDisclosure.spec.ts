import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it } from "vitest"
import { userEvent } from "vitest/browser"
import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import { notePropertyLocation } from "@/routes/noteShowLocation"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper

afterEach(() => {
  wrapper?.unmount()
  document.body.innerHTML = ""
})

it("keeps authored frontmatter when editing the body with details hidden", async () => {
  const note = makeMe.aNote
    .content("---\ntopic: training\nsource: local\n---\n\nOriginal body.")
    .please()
  const save = mockSdkServiceWithImplementation(
    TextContentController,
    "updateNoteContent",
    async (options) =>
      makeMe.aNoteRealm.content(options.body?.content ?? "").please()
  )
  wrapper = mountNoteTextContent(note)
  await flushPromises()

  const row = wrapper.get('[data-testid="rich-note-property-row"]')
    .element as HTMLElement
  expect(row.checkVisibility()).toBe(false)

  const editor = wrapper.get(".ql-editor").element as HTMLElement
  editor.focus()
  editor.innerHTML = "<p>Edited body.</p>"
  editor.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
  editor.blur()
  await flushPromises()

  expect(save).toHaveBeenCalledWith({
    path: { note: note.id },
    body: {
      content: "---\ntopic: training\nsource: local\n---\n\nEdited body.",
    },
  })
})

it.each([false, true])(
  "reveals and collapses mounted details without saving (readonly: %s)",
  async (readonly) => {
    const save = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    const note = makeMe.aNote
      .title("2026-05-15 Trip notes")
      .content("---\nsource: local\ntopic: training\n---\n\nBody.")
      .please()
    wrapper = mountNoteTextContent(note, { readonly })
    await flushPromises()
    expect(wrapper.get('[data-test="note-title"]').text()).toBe(
      "2026-05-15 Trip notes"
    )
    const toggle = wrapper.get('[data-testid="note-details-toggle"]')
    expect(wrapper.get('[data-testid="note-property-summary"]').text()).toBe(
      "Properties: source, topic"
    )
    expect(toggle.attributes("aria-expanded")).toBe("false")
    const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
    expect(rows).toHaveLength(2)
    expect((rows[0]!.element as HTMLElement).checkVisibility()).toBe(false)
    const route = wrapper.vm.$router.currentRoute.value.fullPath
    ;(toggle.element as HTMLElement).focus()
    await userEvent.keyboard("{Enter}")
    await flushPromises()
    expect(toggle.attributes("aria-expanded")).toBe("true")
    expect((rows[0]!.element as HTMLElement).checkVisibility()).toBe(true)
    expect(rows[0]!.text()).toContain("local")
    expect(
      wrapper
        .findAll("section button")
        .filter((button) => button.text() === "Add property")
    ).toHaveLength(readonly ? 0 : 1)
    await toggle.trigger("click")
    expect(toggle.attributes("aria-expanded")).toBe("false")
    expect((rows[0]!.element as HTMLElement).checkVisibility()).toBe(false)
    expect(wrapper.vm.$router.currentRoute.value.fullPath).toBe(route)
    expect(save).not.toHaveBeenCalled()
  }
)

it("keeps the compact line in Markdown mode and resets on another note", async () => {
  wrapper = mountNoteTextContent(
    makeMe.aNote.content("---\ntopic: training\n---\nBody.").please()
  )
  await flushPromises()
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  await wrapper.setProps({
    note: makeMe.aNote.content("Body only.").please(),
    asMarkdown: true,
  })
  expect(wrapper.get('[data-testid="note-property-summary"]').text()).toBe(
    "No properties"
  )
  expect(
    wrapper
      .get('[data-testid="note-details-toggle"]')
      .attributes("aria-expanded")
  ).toBe("false")
  expect(wrapper.find("textarea").exists()).toBe(true)
})

async function openPropertyLocation(key: string) {
  mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  const note = makeMe.aNote.content("---\ntopic: training\n---\nBody.").please()
  wrapper = mountNoteTextContent(note)
  await wrapper.vm.$router.push(notePropertyLocation(note.id, key))
  await flushPromises()
  return wrapper.get('[data-property-key="topic"]')
}

it("reveals and focuses a property location, then allows collapse", async () => {
  const row = await openPropertyLocation("topic")
  expect((row.element as HTMLElement).checkVisibility()).toBe(true)
  expect(row.attributes("data-property-focused")).toBe("true")
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  await flushPromises()
  expect((row.element as HTMLElement).checkVisibility()).toBe(false)
})

it("shows the explicit missing-property notice while keeping details compact", async () => {
  const row = await openPropertyLocation("missing")
  const notice = wrapper.get('[data-testid="rich-note-property-not-found"]')
  expect(notice.text()).toBe('Property "missing" not found')
  expect((notice.element as HTMLElement).checkVisibility()).toBe(true)
  expect((row.element as HTMLElement).checkVisibility()).toBe(false)
})

it("applies a property arrival after resetting another note's disclosure", async () => {
  await openPropertyLocation("topic")
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  const next = makeMe.aNote.content("---\ntopic: training\n---\nBody.").please()
  await wrapper.vm.$router.push(notePropertyLocation(next.id, "topic"))
  await wrapper.setProps({ note: next })
  await flushPromises()
  expect(
    (
      wrapper.get('[data-property-key="topic"]').element as HTMLElement
    ).checkVisibility()
  ).toBe(true)
})
