import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { describe, it, expect, afterEach } from "vitest"

describe("undo editing", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("should call addEditingToUndoHistory on submitChange", async () => {
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage()

    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    storageAccessor.value.refreshNoteRealm(noteRealm)

    const updatedTitle = "updated"
    wrapper = helper
      .component(NoteTextContent)
      .withProps({
        readonly: false,
        note: noteRealm.note,
      })
      .mount({ attachTo: document.body })

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
