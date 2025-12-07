import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import { flushPromises } from "@vue/test-utils"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

describe("undo editing", () => {
  it("should call addEditingToUndoHistory on submitChange", async () => {
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage()

    const noteRealm = makeMe.aNoteRealm.titleConstructor("Dummy Title").please()
    storageAccessor.value.refreshNoteRealm(noteRealm)

    const updatedTitle = "updated"
    const wrapper = helper
      .component(NoteTextContent)
      .withProps({
        readonly: false,
        note: noteRealm.note,
      })
      .mount()

    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.innerText = updatedTitle
    titleEl.dispatchEvent(new Event("input"))
    titleEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(storageAccessor.value.peekUndo()).toMatchObject({
      type: "edit title",
    })
  })
})
