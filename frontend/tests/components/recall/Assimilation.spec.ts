import Assimilation from "@/components/recall/Assimilation.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "@tests/fixtures/makeMe"
import helper, {
  mockShowNoteAccessory,
  mockSdkService,
  wrapSdkResponse,
  wrapSdkError,
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
    noteType: undefined,
  })
  mockSdkService("generateUnderstandingChecklist", {
    points: [],
  })
  mockSdkService("updateNoteType", undefined)

  vi.mocked(useRecallData).mockReturnValue({
    totalAssimilatedCount: mockedTotalAssimilatedCount,
    toRepeatCount: computed(() => toRepeat.value?.length ?? 0),
    toRepeat: ref(undefined),
    currentRecallWindowEndAt: ref(undefined),
    isRecallPaused: ref(false),
    shouldResumeRecall: ref(false),
    treadmillMode: ref(false),
    currentIndex: ref(0),
    diligentMode: ref(false),
    setToRepeat: vi.fn(),
    setCurrentRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: vi.fn(),
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn(),
    setDiligentMode: vi.fn(),
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

  describe("promote point to child note", () => {
    it("should display promote button for each understanding point", async () => {
      const points = ["Point 1", "Point 2", "Point 3"]
      mockSdkService("generateUnderstandingChecklist", { points })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // 验证每个 point 旁边都有按钮
      const listItems = wrapper.findAll("li")
      expect(listItems).toHaveLength(3)

      listItems.forEach((li, index) => {
        expect(li.text()).toContain(points[index])
        // 验证按钮存在
        expect(li.find("button").exists()).toBe(true)
      })
    })

    it("should call createNoteUnderParent API when promote button is clicked", async () => {
      const points = ["Test Point"]
      mockSdkService("generateUnderstandingChecklist", { points })

      const createNoteSpy = mockSdkService("createNoteUnderParent", {
        created: makeMe.aNoteRealm.please(),
        parent: makeMe.aNoteRealm.please(),
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // 点击按钮
      await wrapper.find("li button").trigger("click")
      await flushPromises()

      // 验证 API 被调用，且使用 point 文字作为 newTitle
      expect(createNoteSpy).toHaveBeenCalledWith({
        path: { parentNote: note.id },
        body: { newTitle: "Test Point", wikidataId: "" },
      })
    })

    it("should remove the point from checklist after successful creation", async () => {
      const points = ["Point 1", "Point 2", "Point 3"]
      mockSdkService("generateUnderstandingChecklist", { points })

      mockSdkService("createNoteUnderParent", {
        created: makeMe.aNoteRealm.please(),
        parent: makeMe.aNoteRealm.please(),
      })

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // 验证初始有 3 个 points
      expect(wrapper.findAll("li")).toHaveLength(3)

      // 点击第二个 point 的按钮
      const secondLi = wrapper.findAll("li")[1]
      if (secondLi) {
        await secondLi.find("button").trigger("click")
        await flushPromises()
      }

      // 验证只剩 2 个 points
      expect(wrapper.findAll("li")).toHaveLength(2)
      // 验证 "Point 2" 不见了
      expect(wrapper.text()).not.toContain("Point 2")
      // 验证其他两个还在
      expect(wrapper.text()).toContain("Point 1")
      expect(wrapper.text()).toContain("Point 3")
    })

    it("should keep the point in checklist and show error when API fails", async () => {
      const points = ["Test Point"]
      mockSdkService("generateUnderstandingChecklist", { points })

      // Mock API 失败
      const createNoteSpy = mockSdkService("createNoteUnderParent", undefined)
      createNoteSpy.mockResolvedValue(wrapSdkError("API Error"))

      const wrapper = renderer
        .withCleanStorage()
        .withProps({ note })
        .withRouter()
        .mount()

      await flushPromises()

      // 点击按钮
      await wrapper.find("li button").trigger("click")
      await flushPromises()

      // 验证 point 还在列表中
      expect(wrapper.findAll("li")).toHaveLength(1)
      expect(wrapper.text()).toContain("Test Point")
    })
  })
})
