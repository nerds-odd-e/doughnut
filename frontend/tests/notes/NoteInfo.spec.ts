import NoteInfoBar from "@/components/notes/NoteInfoBar.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"

const stubResponse: NoteInfo = {
  memoryTrackers: [makeMe.aMemoryTracker.please()],
  note: makeMe.aNoteRealm.please(),
  createdAt: "",
}

describe("note info", () => {
  it("should render values", async () => {
    const getNoteInfoSpy = mockSdkService("getNoteInfo", stubResponse)
    const wrapper = helper
      .component(NoteInfoBar)
      .withProps({
        noteId: 123,
      })
      .withRouter()
      .mount()
    await flushPromises()
    expect(wrapper.findAll(".statistics-value")).toHaveLength(3)
    expect(getNoteInfoSpy).toBeCalledWith({
      path: { note: 123 },
    })
  })
})
