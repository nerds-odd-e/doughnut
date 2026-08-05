import {
  MemoryTrackerController,
  NoteController,
  RecallsController,
} from "@generated/doughnut-backend-api/sdk.gen"
import { useRecallData } from "@/composables/useRecallData"
import RecallPage from "@/pages/RecallPage.vue"
import type { MemoryTrackerLite } from "@generated/doughnut-backend-api"
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

const createUseRecallDataMock = (toRepeat: MemoryTrackerLite[]) => {
  const toRepeatRef = ref<MemoryTrackerLite[] | undefined>(toRepeat)
  return {
    toRepeatCount: computed(() => toRepeatRef.value?.length ?? 0),
    toRepeat: toRepeatRef,
    currentRecallWindowEndAt: ref(undefined),
    totalAssimilatedCount: ref(0),
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
    dueRecallsRefreshNonce: ref(0),
    requestDueRecallsRefresh: vi.fn(),
  }
}

afterEach(() => {
  document.body.innerHTML = ""
})

describe("overlap try-again stay and retry", () => {
  const memoryTrackerId = 123
  let getThresholdExceededSpy: ReturnType<typeof mockSdkService>
  let askAQuestionSpy: ReturnType<typeof mockSdkService>

  mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)

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
      { thresholdExceeded: false }
    )
    vi.mocked(useRecallData).mockReturnValue(
      createUseRecallDataMock([
        { memoryTrackerId, spelling: true },
        { memoryTrackerId: 456, spelling: true },
      ])
    )
  })

  it("stays on the same tracker, skips threshold, and remounts spelling on Try again", async () => {
    const overlapResult = makeMe.anAnsweredQuestion
      .overlap("Shared Title")
      .withMemoryTrackerId(memoryTrackerId)
      .please()

    const wrapper = helper
      .component(RecallPage)
      .withCleanStorage()
      .withProps({ eagerFetchCount: 1 })
      .currentRoute({ name: "recall" })
      .mount()
    await flushPromises()

    type ExposedVM = { currentIndex: number }
    const vm = wrapper.vm as unknown as ExposedVM
    wrapper.findComponent({ name: "Quiz" }).vm.$emit("answered", overlapResult)
    await flushPromises()

    expect(vm.currentIndex).toBe(0)
    expect(getThresholdExceededSpy).not.toHaveBeenCalled()

    const askCallsBeforeRetry = askAQuestionSpy.mock.calls.length
    await wrapper.find('[data-testid="overlap-try-again"]').trigger("click")
    await flushPromises()

    expect(
      wrapper.findComponent({ name: "AnsweredSpellingQuestion" }).exists()
    ).toBe(false)
    expect(vm.currentIndex).toBe(0)
    expect(
      wrapper.findComponent({ name: "Quiz" }).props("spellingRetryNonce")
    ).toBe(1)
    expect(askAQuestionSpy.mock.calls.length).toBeGreaterThan(
      askCallsBeforeRetry
    )
  })
})
