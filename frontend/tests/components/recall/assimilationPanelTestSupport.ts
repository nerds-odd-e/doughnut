import {
  AiController,
  AssimilationController,
  AssimilationSequenceSkipController,
  NoteController,
} from "@generated/donut-backend-api/sdk.gen"
import AssimilationPanel from "@/components/recall/AssimilationPanel.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import helper, { mockSdkService, productionRouterAt } from "@tests/helpers"
import RenderingHelper from "@tests/helpers/RenderingHelper"
import { useRecallData } from "@/composables/useRecallData"
import { useAssimilationCount } from "@/composables/useAssimilationCount"
import usePopups from "@/components/commons/Popups/usePopups"
import { closeButtonEl } from "@tests/commons/modalTestSupport"
import { afterEach, beforeEach, expect, vi } from "vitest"
import { refinementLayoutItems } from "./noteRefinementTestSupport"
import { resetRecallData } from "@tests/helpers/recallDataTestSupport"
import { noteShowLocation } from "@/routes/noteShowLocation"
import type { Router } from "vue-router"

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

export const nextNoteId = note.id + 1

let router: Router
export const expectAtNoteOf = (noteId: number) =>
  expect(router.currentRoute.value).toMatchObject(noteShowLocation(noteId))

let renderer: RenderingHelper<typeof AssimilationPanel>
export let assimilateSpy: ReturnType<typeof mockSdkService>
export let skipSequenceSpy: ReturnType<typeof mockSdkService>

const recallData = useRecallData()
export const { totalAssimilatedCount } = recallData
let refreshNonceAtStart = 0
export const dueRecallsRefreshRequested = () =>
  recallData.dueRecallsRefreshNonce.value > refreshNonceAtStart

const assimilationCount = useAssimilationCount()
export const { assimilatedCountOfTheDay } = assimilationCount

export function setupAssimilationPanelTests() {
  afterEach(() => {
    document.body.innerHTML = ""
    vi.clearAllMocks()
  })

  beforeEach(() => {
    resetRecallData()
    recallData.setTotalAssimilatedCount(0)
    refreshNonceAtStart = recallData.dueRecallsRefreshNonce.value
    assimilationCount.setAssimilatedCountOfTheDay(0)
    assimilationCount.setDueCount(0)
    assimilationCount.setTotalUnassimilatedCount(0)

    assimilateSpy = mockSdkService(AssimilationController, "assimilate", [])
    skipSequenceSpy = mockSdkService(
      AssimilationSequenceSkipController,
      "create",
      { id: 1 }
    )
    mockSdkService(AssimilationController, "next", {
      nextUnit: { noteId: nextNoteId },
    })
    mockSdkService(NoteController, "getNoteInfo", {})
    mockSdkService(AiController, "generateRefinementSuggestions", {
      items: refinementLayoutItems([]),
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
  router = await productionRouterAt(noteShowLocation(note.id))
  const wrapper = renderer
    .withCleanStorage()
    .withProps({ note })
    .withRouter(router)
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
