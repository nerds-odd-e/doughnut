import NoteInfoBar from "@/components/notes/NoteInfoBar.vue"
import { flushPromises } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"
import * as sdk from "@generated/backend/sdk.gen"

const stubResponse: NoteInfo = {
  memoryTrackers: [makeMe.aMemoryTracker.please()],
  note: makeMe.aNoteRealm.please(),
  createdAt: "",
}

describe("note info", () => {
  it("should render values", async () => {
    vi.spyOn(sdk, "getNoteInfo").mockResolvedValue({
      data: stubResponse,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
    const wrapper = helper
      .component(NoteInfoBar)
      .withProps({
        noteId: 123,
      })
      .mount()
    await flushPromises()
    expect(wrapper.findAll(".statistics-value")).toHaveLength(3)
    expect(sdk.getNoteInfo).toBeCalledWith({
      path: { note: 123 },
    })
  })
})
