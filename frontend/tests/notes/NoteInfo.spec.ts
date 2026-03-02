import NoteInfoBar from "@/components/notes/NoteInfoBar.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"
import { describe, it, expect, afterEach } from "vitest"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

const noteRealm = makeMe.aNoteRealm.please()
const stubResponse: NoteInfo = {
  memoryTrackers: [makeMe.aMemoryTracker.please()],
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
    useStorageAccessor().value?.refreshNoteRealm(noteRealm)

    wrapper = helper
      .component(NoteInfoBar)
      .withProps({
        noteId: noteRealm.id,
      })
      .withRouter()
      .mount({ attachTo: document.body })
    await flushPromises()
    expect(wrapper.find("table").exists()).toBe(true)
    expect(getNoteInfoSpy).toBeCalledWith({
      path: { note: noteRealm.id },
    })
  })
})
