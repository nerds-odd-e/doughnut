import { TextContentController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it } from "vitest"
import {
  noteDetailsDisclosureKey,
  type NoteDetailsDisclosure,
} from "@/composables/noteDetailsDisclosure"
import { mockSdkServiceWithImplementation } from "@tests/helpers"
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

  // The Structure slice has no disclosure control yet; collapse its provided state.
  const instance = wrapper.vm.$ as unknown as {
    provides: Record<symbol, NoteDetailsDisclosure>
  }
  const state = instance.provides[noteDetailsDisclosureKey as symbol]!
  state.revealed.value = false
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
