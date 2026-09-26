import {
  AiController,
  AssimilationController,
  AssimilationSequenceSkipController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import type { MemoryTrackerLite } from "@generated/donut-backend-api"
import { computed, ref } from "vue"
import usePopups from "@/components/commons/Popups/usePopups"
import { closeButtonEl } from "@tests/commons/modalTestSupport"
import { afterEach, beforeEach, vi } from "vitest"
import { mockedGoToNextAssimilation } from "./assimilationPanelMocks"
import { refinementLayoutItems } from "./noteRefinementTestSupport"

export const assimilateButtonSelector =
  '[data-test="assimilate-UNDERSTANDING"]' as const
export const assimilateAsCommissionedButtonSelector =
  '[data-test="assimilate-COMMISSIONED"]' as const
export const rememberSpellingButtonSelector =
  '[data-test="assimilate-SPELLING"]' as const
export const commissionedStatusSelector =
  '[data-test="assimilation-status-COMMISSIONED"]' as const
export const spellingStatusSelector =
  '[data-test="assimilation-status-SPELLING"]' as const
export const understandingStatusSelector =
  '[data-test="assimilation-status-UNDERSTANDING"]' as const

const buttonEl = (wrapper: VueWrapper, selector: string) =>
  wrapper.element.querySelector(selector) as HTMLInputElement | null

export const assimilateButtonEl = (wrapper: VueWrapper) =>
  buttonEl(wrapper, assimilateButtonSelector)
export const skipButtonEl = (wrapper: VueWrapper) =>
  buttonEl(wrapper, '[data-test="skip"]')
export const returnToSequenceButtonEl = (wrapper: VueWrapper) =>
  buttonEl(wrapper, '[data-test="return-to-sequence"]')

async function clickAndSettle(button: HTMLElement | null) {
  button!.click()
  await flushPromises()
}

export const clickAssimilate = (wrapper: VueWrapper) =>
  clickAndSettle(assimilateButtonEl(wrapper))
export const clickReturnToSequence = (wrapper: VueWrapper) =>
  clickAndSettle(returnToSequenceButtonEl(wrapper))
export const clickAssimilateAsCommissioned = (wrapper: VueWrapper) =>
  clickAndSettle(buttonEl(wrapper, assimilateAsCommissionedButtonSelector))
export const clickRememberSpelling = (wrapper: VueWrapper) =>
  clickAndSettle(buttonEl(wrapper, rememberSpellingButtonSelector))

export async function clickSkipAndConfirm(wrapper: VueWrapper) {
  skipButtonEl(wrapper)!.click()
  usePopups().popups.done(true)
  await flushPromises()
}

export const { note } = makeMe.aMemoryTracker
  .ofNote(makeMe.aNoteRealm.please())
  .please()

let renderer: RenderingHelper<typeof AssimilationPanel>
export let assimilateSpy: ReturnType<typeof mockSdkService>
export let skipSequenceSpy: ReturnType<typeof mockSdkService>

export const mockedRequestDueRecallsRefresh = vi.fn()
export const mockedTotalAssimilatedCount = ref(0)
const toRepeat = ref<MemoryTrackerLite[] | undefined>(undefined)

const assimilationCount = useAssimilationCount()
export const { assimilatedCountOfTheDay } = assimilationCount

export function setupAssimilationPanelTests() {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
    const popups = usePopups()
    while (popups.popups.peek().length) {
      popups.popups.done(false)
    }
  })

  beforeEach(() => {
    mockedGoToNextAssimilation.mockClear()
    mockedGoToNextAssimilation.mockResolvedValue(true)
    mockedTotalAssimilatedCount.value = 0
    assimilationCount.setAssimilatedCountOfTheDay(0)
    assimilationCount.setDueCount(0)
    assimilationCount.setTotalUnassimilatedCount(0)

    assimilateSpy = mockSdkService(AssimilationController, "assimilate", [])
    skipSequenceSpy = mockSdkService(
      AssimilationSequenceSkipController,
      "create",
      { id: 1 }
    )
    mockSdkService(NoteController, "getNoteInfo", {})
    mockSdkService(AiController, "generateRefinementSuggestions", {
      items: refinementLayoutItems([]),
    })

    vi.mocked(useRecallData).mockReturnValue({
      totalAssimilatedCount: mockedTotalAssimilatedCount,
      toRepeatCount: computed(() => toRepeat.value?.length ?? 0),
      toRepeat: ref(undefined),
      dueCommissioned: ref(undefined),
      potentialLearningSessions: computed(() => []),
      currentRecallWindowEndAt: ref(undefined),
      isRecallPaused: ref(false),
      isViewingAnsweredQuestion: ref(false),
      shouldResumeRecall: ref(false),
      treadmillMode: ref(false),
      currentIndex: ref(0),
      diligentMode: ref(false),
      setToRepeat: vi.fn(),
      setDueCommissioned: vi.fn(),
      setCurrentRecallWindowEndAt: vi.fn(),
      setTotalAssimilatedCount: vi.fn(),
      setIsRecallPaused: vi.fn(),
      setIsViewingAnsweredQuestion: vi.fn(),
      clearShouldResumeRecall: vi.fn(),
      setTreadmillMode: vi.fn(),
      setCurrentIndex: vi.fn(),
      setDiligentMode: vi.fn(),
      dueRecallsRefreshNonce: ref(0),
      requestDueRecallsRefresh: mockedRequestDueRecallsRefresh,
    })

    renderer = helper.component(AssimilationPanel)
  })
}

export function opaqueContentBlockerEl() {
  return document.body.querySelector(
    '[data-test="opaque-content-blocker"]'
  ) as HTMLElement | null
}

export function spellingVerificationPopupEl() {
  return document.body.querySelector(
    '[data-test="spelling-verification-popup"]'
  ) as HTMLElement | null
}

export async function mountAssimilationPanelReady() {
  const wrapper = renderer
    .withCleanStorage()
    .withProps({ note })
    .withRouter()
    .mount()
  await flushPromises()
  return wrapper
}

export async function closeSpellingVerificationPopup() {
  closeButtonEl()!.click()
  await flushPromises()
}

export const clickVerifySpelling = () =>
  clickAndSettle(
    document.body.querySelector('[data-test="verify-spelling"]') as HTMLElement
  )
