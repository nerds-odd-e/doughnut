import Assimilation from "@/components/review/Assimilation.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { ref } from "vue"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")

let renderer: RenderingHelper<typeof Assimilation>
const mockedAssimilateCall = vi.fn()
const mockedGetNoteCall = vi.fn()
const mockedGetNoteInfoCall = vi.fn()
const mockedIncrementAssimilatedCount = vi.fn()
const mockedTotalAssimilatedCount = ref(0)

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  vi.spyOn(helper.managedApi.services, "assimilate").mockImplementation(
    mockedAssimilateCall
  )
  vi.spyOn(helper.managedApi.services, "showNote").mockImplementation(
    mockedGetNoteCall
  )
  vi.spyOn(helper.managedApi.services, "getNoteInfo").mockImplementation(
    mockedGetNoteInfoCall
  )

  vi.mocked(useRecallData).mockReturnValue({
    totalAssimilatedCount: mockedTotalAssimilatedCount,
    toRepeatCount: ref(0),
    recallWindowEndAt: ref(undefined),
    setToRepeatCount: vi.fn(),
    setRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    decrementToRepeatCount: vi.fn(),
  })

  vi.mocked(useAssimilationCount).mockReturnValue({
    incrementAssimilatedCount: mockedIncrementAssimilatedCount,
    dueCount: ref(0),
    setDueCount: vi.fn(),
    assimilatedCountOfTheDay: ref(0),
    setAssimilatedCountOfTheDay: vi.fn(),
    totalUnassimilatedCount: ref(0),
    setTotalUnassimilatedCount: vi.fn(),
  })

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
    it("emits initialReviewDone and increments counts correctly when assimilating normally", async () => {
      const returnedTrackers = [
        { id: 1, removedFromTracking: false },
        { id: 2, removedFromTracking: true },
        { id: 3, removedFromTracking: false },
      ]
      mockedAssimilateCall.mockResolvedValue(returnedTrackers)
      const wrapper = renderer
        .withStorageProps({
          note,
        })
        .mount()

      await flushPromises()
      await wrapper.find('input[value="Keep for repetition"]').trigger("click")
      await flushPromises()

      expect(mockedAssimilateCall).toHaveBeenCalledWith({
        requestBody: {
          noteId: note.id,
          skipMemoryTracking: false,
        },
      })
      expect(wrapper.emitted()).toHaveProperty("initialReviewDone")
      expect(mockedTotalAssimilatedCount.value).toBe(2)
      expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(2)
    })
  })
})
