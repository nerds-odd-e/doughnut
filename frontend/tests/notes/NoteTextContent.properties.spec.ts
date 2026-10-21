import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it } from "vitest"
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

const top = (selector: string) =>
  wrapper.get(selector).element.getBoundingClientRect().top

const addPropertyButtons = () =>
  wrapper
    .findAll("section button")
    .filter((button) => button.text() === "Add property")

it("keeps authored frontmatter when editing the body", async () => {
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
  "presents scalar and list property rows above the body on arrival (readonly: %s)",
  async (readonly) => {
    const save = mockSdkService(
      TextContentController,
      "updateNoteContent",
      makeMe.aNoteRealm.please()
    )
    const note = makeMe.aNote
      .title("2026-05-15 Trip notes")
      .content("---\nsource: local\ntags:\n  - travel\n  - food\n---\n\nBody.")
      .please()
    wrapper = mountNoteTextContent(note, { readonly })
    await flushPromises()
    expect(wrapper.get('[data-test="note-title"]').text()).toBe(
      "2026-05-15 Trip notes"
    )
    const rows = wrapper.findAll('[data-testid="rich-note-property-row"]')
    expect(rows).toHaveLength(2)
    expect((rows[0]!.element as HTMLElement).checkVisibility()).toBe(true)
    expect(rows[0]!.text()).toContain("local")
    expect(rows[1]!.text()).toContain("travel")
    expect(rows[1]!.text()).toContain("food")
    expect(rows[1]!.element.getBoundingClientRect().bottom).toBeLessThanOrEqual(
      top(".ql-editor")
    )
    expect(addPropertyButtons()).toHaveLength(readonly ? 0 : 1)
    expect(save).not.toHaveBeenCalled()
  }
)

it("offers Add property above the body of an editable note without properties", async () => {
  wrapper = mountNoteTextContent(makeMe.aNote.content("Body.").please())
  await flushPromises()
  const [add] = addPropertyButtons()
  expect((add!.element as HTMLElement).checkVisibility()).toBe(true)
  expect(add!.element.getBoundingClientRect().bottom).toBeLessThanOrEqual(
    top(".ql-editor")
  )
})

it("proceeds from the title directly to the body of a read-only note without properties", async () => {
  wrapper = mountNoteTextContent(makeMe.aNote.content("Body.").please(), {
    readonly: true,
  })
  await flushPromises()
  const region = wrapper.get('[aria-label="Note content"]').element
  const body = wrapper.get(".ql-editor p").element
  expect(body.textContent).toBe("Body.")
  expect(
    body.getBoundingClientRect().top - region.getBoundingClientRect().top
  ).toBeLessThan(24)
})

async function openPropertyLocation(key: string) {
  mockSdkService(NoteController, "getNoteInfo", { memoryTrackers: [] })
  const note = makeMe.aNote.content("---\ntopic: training\n---\nBody.").please()
  wrapper = mountNoteTextContent(note)
  await wrapper.vm.$router.push(notePropertyLocation(note.id, key))
  await flushPromises()
  return wrapper.get('[data-property-key="topic"]')
}

it("focuses the property at a property location", async () => {
  const row = await openPropertyLocation("topic")
  expect((row.element as HTMLElement).checkVisibility()).toBe(true)
  expect(row.attributes("data-property-focused")).toBe("true")
})

it("shows the explicit missing-property notice beside the existing rows", async () => {
  const row = await openPropertyLocation("missing")
  const notice = wrapper.get('[data-testid="rich-note-property-not-found"]')
  expect(notice.text()).toBe('Property "missing" not found')
  expect((notice.element as HTMLElement).checkVisibility()).toBe(true)
  expect((row.element as HTMLElement).checkVisibility()).toBe(true)
})

it("focuses the property location on the next note", async () => {
  await openPropertyLocation("topic")
  const next = makeMe.aNote.content("---\ntopic: training\n---\nBody.").please()
  await wrapper.vm.$router.push(notePropertyLocation(next.id, "topic"))
  await wrapper.setProps({ note: next })
  await flushPromises()
  const row = wrapper.get('[data-property-key="topic"]')
  expect((row.element as HTMLElement).checkVisibility()).toBe(true)
  expect(row.attributes("data-property-focused")).toBe("true")
})
