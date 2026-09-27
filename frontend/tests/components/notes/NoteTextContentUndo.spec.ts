import NoteTextContent from "@/components/notes/core/NoteTextContent.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { resetNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import { describe, it, expect, afterEach } from "vitest"

describe("undo editing", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("should call addEditingToUndoHistory on submitChange", async () => {
    const noteStore = useNoteStore()
    resetNoteStore()

    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
    noteStore.refreshNoteRealm(noteRealm)

    const updatedTitle = "updated"
    wrapper = helper
      .component(NoteTextContent)
      .withRouter()
      .withProps({
        readonly: false,
        note: noteRealm.note,
        wikiLinks: [],
      })
      .mount({ attachTo: document.body })

    const titleEl = wrapper.find('[role="title"]').element as HTMLElement
    titleEl.innerText = updatedTitle
    titleEl.dispatchEvent(new Event("input"))
    titleEl.dispatchEvent(new Event("blur"))
    await flushPromises()

    expect(noteStore.peekUndo()).toMatchObject({
      type: "edit title",
    })
  })
})
