import NoteInfoBar from "@/components/notes/NoteInfoBar.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"

const stubResponse: NoteInfo = {
  memoryTrackers: [makeMe.aMemoryTracker.please()],
  note: makeMe.aNoteRealm.please(),
  createdAt: "",
}

describe("note info", () => {
  it("should render values", async () => {
    helper.managedApi.restNoteController.getNoteInfo = vi
      .fn()
      .mockResolvedValue(stubResponse)
    const wrapper = helper
      .component(NoteInfoBar)
      .withProps({
        noteId: 123,
      })
      .mount()
    await flushPromises()
    expect(wrapper.findAll(".statistics-value")).toHaveLength(3)
    expect(helper.managedApi.restNoteController.getNoteInfo).toBeCalledWith(123)
  })
})
