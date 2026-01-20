import NoteInfoBar from "@/components/notes/NoteInfoBar.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"
import { describe, it, expect, afterEach } from "vitest"

const stubResponse: NoteInfo = {
  memoryTrackers: [makeMe.aMemoryTracker.please()],
  note: makeMe.aNoteRealm.please(),
  createdAt: "",
}

describe("note info", () => {
  let wrapper: VueWrapper

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("should render values", async () => {
    const getNoteInfoSpy = mockSdkService("getNoteInfo", stubResponse)
    wrapper = helper
      .component(NoteInfoBar)
      .withProps({
        noteId: 123,
      })
      .withRouter()
      .mount({ attachTo: document.body })
    await flushPromises()
    expect(wrapper.findAll(".statistics-value")).toHaveLength(3)
    expect(getNoteInfoSpy).toBeCalledWith({
      path: { note: 123 },
    })
  })
})
