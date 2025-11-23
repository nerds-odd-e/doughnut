import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import { flushPromises } from "@vue/test-utils"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("undo editing", () => {
  it("should call addEditingToUndoHistory on submitChange", async () => {
    const histories = createNoteStorage()

    const noteRealm = makeMe.aNoteRealm.topicConstructor("Dummy Title").please()
    histories.refreshNoteRealm(noteRealm)

    const updatedTitle = "updated"
    const wrapper = helper
      .component(NoteTextContent)
      .withProps({
        readonly: false,
        note: noteRealm.note,
        storageAccessor: histories,
      })
      .mount()

    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.innerText = updatedTitle
    titleEl.dispatchEvent(new Event("input"))
    titleEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(histories.peekUndo()).toMatchObject({ type: "edit title" })
  })
})
