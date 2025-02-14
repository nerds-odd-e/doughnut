import Assimilation from "@/components/review/Assimilation.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import usePopups from "@/components/commons/Popups/usePopups"

let renderer: RenderingHelper<typeof Assimilation>
const mockedAssimilateCall = vi.fn()
const mockedGetNoteCall = vi.fn()
const mockedGetNoteInfoCall = vi.fn()

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  helper.managedApi.assimilationController.assimilate = mockedAssimilateCall
  helper.managedApi.restNoteController.show = mockedGetNoteCall
  helper.managedApi.restNoteController.getNoteInfo = mockedGetNoteInfoCall
  renderer = helper.component(Assimilation)
})

describe("Assimilation component", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
  const { note } = memoryTracker

  beforeEach(() => {
    mockedGetNoteCall.mockResolvedValue(noteRealm)
    mockedGetNoteInfoCall.mockResolvedValue({})
  })

  describe("normal assimilation", () => {
    it("emits initialReviewDone when assimilating normally", async () => {
      mockedAssimilateCall.mockResolvedValue({})
      const wrapper = renderer
        .withStorageProps({
          note,
        })
        .mount()

      await flushPromises()
      await wrapper.find('input[value="Keep for repetition"]').trigger("click")
      await flushPromises()

      expect(mockedAssimilateCall).toHaveBeenCalledWith({
        noteId: note.id,
        skipMemoryTracking: false,
      })
      expect(wrapper.emitted()).toHaveProperty("initialReviewDone")
    })
  })
})
