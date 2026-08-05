import {
  MemoryTrackerController,
  NoteController,
  RecallsController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import type {
  AnsweredQuestion,
  MemoryTrackerLite,
} from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import mockBrowserTimeZone from "@tests/helpers/mockBrowserTimeZone"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { computed, ref } from "vue"

vi.mock("@/composables/useRecallData")
vi.mock("@/components/commons/Popups/usePopups")

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRoute: () => ({ path: "/", fullPath: "/" }),
    useRouter: () => ({ currentRoute: { value: { name: "recall" } } }),
  }
})

const createUseRecallDataMock = (overrides?: {
  toRepeat?: MemoryTrackerLite[]
}) => {
  const toRepeatRef = ref<MemoryTrackerLite[] | undefined>(overrides?.toRepeat)
  const treadmillModeRef = ref(false)
  const currentIndexRef = ref(0)
  const diligentModeRef = ref(false)
  const dueRecallsRefreshNonce = ref(0)
  return {
    toRepeatCount: computed(() => toRepeatRef.value?.length ?? 0),
    toRepeat: toRepeatRef,
    currentRecallWindowEndAt: ref(undefined),
    totalAssimilatedCount: ref(0),
    isRecallPaused: ref(false),
    shouldResumeRecall: ref(false),
    treadmillMode: treadmillModeRef,
    currentIndex: currentIndexRef,
    diligentMode: diligentModeRef,
    setToRepeat: vi.fn((trackers: MemoryTrackerLite[] | undefined) => {
      toRepeatRef.value = trackers
    }),
    setCurrentRecallWindowEndAt: vi.fn(),
    setTotalAssimilatedCount: vi.fn(),
    setIsRecallPaused: vi.fn(),
    resumeRecall: vi.fn(),
    clearShouldResumeRecall: vi.fn(),
    setTreadmillMode: vi.fn(),
    setCurrentIndex: vi.fn((index: number) => {
      currentIndexRef.value = index
    }),
    setDiligentMode: vi.fn(),
    dueRecallsRefreshNonce,
    requestDueRecallsRefresh: vi.fn(),
  }
}

afterEach(() => {
  document.body.innerHTML = ""
})

describe("overlap try-again stay and retry", () => {
  const createMemoryTrackerLite = (
    id: number,
    spelling = false
  ): MemoryTrackerLite => ({
    memoryTrackerId: id,
    spelling,
  })

  const mountPage = async () => {
    const wrapper = helper
      .component(RecallPage)
      .withCleanStorage()
      .withProps({ eagerFetchCount: 1 })
      .currentRoute({ name: "recall" })
      .mount()
    await flushPromises()
    return wrapper
  }

  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)

  const memoryTrackerId = 123
  let getThresholdExceededSpy: ReturnType<typeof mockSdkService>
  let askAQuestionSpy: ReturnType<typeof mockSdkService>

  beforeEach(() => {
    vi.resetAllMocks()
    vi.useFakeTimers()
    mockSdkService(NoteController, "showNote", makeMe.aNoteRealm.please())
    mockSdkService(
      RecallsController,
      "recalling",
      makeMe.aDueMemoryTrackersList.please()
    )
    mockSdkService(RecallsController, "previouslyAnswered", [])
    mockSdkService(
      MemoryTrackerController,
      "showMemoryTracker",
      makeMe.aMemoryTracker.please()
    )
    askAQuestionSpy = mockSdkService(
      MemoryTrackerController,
      "askAQuestion",
      makeMe.aRecallQuestion.withSpellingStem("Spell").please()
    )
    getThresholdExceededSpy = mockSdkService(
      MemoryTrackerController,
      "getThresholdExceeded",
      {
        thresholdExceeded: false,
      }
    )
    const trackers = [
      createMemoryTrackerLite(memoryTrackerId, true),
      createMemoryTrackerLite(456, true),
    ]
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock({ toRepeat: trackers })
    )
  })

  it("stays on the same tracker, skips threshold, and remounts spelling on Try again", async () => {
    const note = makeMe.aNote.please()
    const overlapResult: AnsweredQuestion = {
      ...makeMe.anAnsweredQuestion
        .withNote(note)
        .spelling()
        .withAnswer({
          id: 1,
          correct: false,
          spellingAnswer: "Shared Title",
          outcome: "OVERLAP",
        })
        .withMemoryTrackerId(memoryTrackerId)
        .please(),
    }

    const wrapper = await mountPage()
    await flushPromises()
    type ExposedVM = { currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    expect(vm.currentIndex).toBe(0)

    const quizBefore = wrapper.findComponent({ name: "Quiz" })
    expect(quizBefore.exists()).toBe(true)
    quizBefore.vm.$emit("answered", overlapResult)
    await flushPromises()

    expect(vm.currentIndex).toBe(0)
    expect(getThresholdExceededSpy).not.toHaveBeenCalled()

    const answeredSpelling = wrapper.findComponent({
      name: "AnsweredSpellingQuestion",
    })
    expect(answeredSpelling.exists()).toBe(true)

    const tryAgain = answeredSpelling.find('[data-testid="overlap-try-again"]')
    expect(tryAgain.exists()).toBe(true)
    const askCallsBeforeRetry = askAQuestionSpy.mock.calls.length
    await tryAgain.trigger("click")
    await flushPromises()

    expect(
      wrapper.findComponent({ name: "AnsweredSpellingQuestion" }).exists()
    ).toBe(false)
    expect(vm.currentIndex).toBe(0)
    const quizAfter = wrapper.findComponent({ name: "Quiz" })
    expect(quizAfter.exists()).toBe(true)
    expect(quizAfter.props("spellingRetryNonce")).toBe(1)
    expect(
      quizAfter.findComponent({ name: "SpellingQuestionDisplay" }).exists()
    ).toBe(true)
    expect(askAQuestionSpy.mock.calls.length).toBeGreaterThan(
      askCallsBeforeRetry
    )
  })
})
