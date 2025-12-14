import Assimilation from "@/components/recall/Assimilation.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  wrapSdkResponse,
} from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import { computed, ref } from "vue"

vi.mock("@/composables/useRecallData")
vi.mock("@/composables/useAssimilationCount")

let renderer: RenderingHelper<typeof Assimilation>
let assimilateSpy: ReturnType<typeof mockSdkService<"assimilate">>
let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>
const mockedIncrementAssimilatedCount = vi.fn()
const mockedTotalAssimilatedCount = ref(0)
const toRepeat = ref<
  Array<{ memoryTrackerId?: number; spelling?: boolean }> | undefined
>(undefined)

afterEach(() => {
  document.body.innerHTML = ""
  vi.clearAllMocks()
})

beforeEach(() => {
  assimilateSpy = mockSdkService("assimilate", [])
  showNoteSpy = mockSdkService("showNote", makeMe.aNoteRealm.please())
  mockSdkService("getNoteInfo", {
    note: makeMe.aNoteRealm.please(),
    createdAt: "",
    noteType: "unassigned",
  })
  mockSdkService("generateSummary", {
    points: [],
  })
  mockSdkService("updateNoteType", undefined)

  vi.mocked(useRecallData).mockReturnValue({
    totalAssimilatedCount: mockedTotalAssimilatedCount,
    toRepeatCount: computed(() => toRepeat.value?.length ?? 0),
    toRepeat: ref(undefined),
    isRecallPaused: ref(false),
    shouldResumeRecall: ref(false),
    treadmillMode: ref(false),
    currentIndex: ref(0),
    setToRepeat: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: vi.fn(),
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn(),
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

  mockShowNoteAccessory()
  renderer = helper.component(Assimilation)
})

describe("Assimilation component", () => {
  const noteRealm = makeMe.aNoteRealm.please()
  const memoryTracker = makeMe.aMemoryTracker.ofNote(noteRealm).please()
  const { note } = memoryTracker

  beforeEach(() => {
    showNoteSpy.mockResolvedValue(wrapSdkResponse(noteRealm))
  })

  describe("normal assimilation", () => {
    it("emits initialReviewDone and increments counts correctly when assimilating normally", async () => {
      const returnedTrackers = [
        { id: 1, removedFromTracking: false },
        { id: 2, removedFromTracking: true },
        { id: 3, removedFromTracking: false },
      ]
      assimilateSpy.mockResolvedValue(wrapSdkResponse(returnedTrackers))
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note,
        })
        .withRouter()
        .mount()

      await flushPromises()
      await wrapper.find('input[value="Keep for repetition"]').trigger("click")
      await flushPromises()

      expect(assimilateSpy).toHaveBeenCalledWith({
        body: {
          noteId: note.id,
          skipMemoryTracking: false,
        },
      })
      expect(wrapper.emitted()).toHaveProperty("initialReviewDone")
      expect(mockedTotalAssimilatedCount.value).toBe(2)
      expect(mockedIncrementAssimilatedCount).toHaveBeenCalledWith(2)
    })
  })

  describe("note type selection via NoteInfoBar", () => {
    it("displays note type selection in NoteInfoBar", async () => {
      const wrapper = renderer
        .withCleanStorage()
        .withProps({
          note,
        })
        .withRouter()
        .mount()

      await flushPromises()

      // Note type selection is now rendered by NoteInfoComponent via NoteInfoBar
      const noteTypeSelection = wrapper.find(
        '[data-test="note-type-selection-dialog"]'
      )
      expect(noteTypeSelection.exists()).toBe(true)
    })
  })
})
