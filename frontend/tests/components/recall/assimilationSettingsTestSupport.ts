import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import AssimilationSettings from "@/components/recall/AssimilationSettings.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import {
  assimilationPropertyRow,
  clickPropertyAssimilate,
  expandAssimilationPropertiesSection,
  noteWithAssimilationProperties,
  propertyRowControl,
} from "./assimilationPropertyTestSupport"
import {
  assimilateButtonSelector,
  removeFromRecallButtonSelector,
  returnToSequenceButtonSelector,
  reviveButtonSelector,
  skipButtonSelector,
} from "./assimilationPanelTestSupport"
import { afterEach, beforeEach } from "vitest"

export let wrapper: VueWrapper
export let getNoteInfoSpy: ReturnType<typeof mockSdkService>

export function setupAssimilationSettingsTests() {
  beforeEach(() => {
    getNoteInfoSpy = mockSdkService(NoteController, "getNoteInfo", {})
  })

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })
}

export async function mountAssimilationSettingsReady() {
  wrapper = helper
    .component(AssimilationSettings)
    .withProps({
      note: noteWithAssimilationProperties,
      noteInfoLoaded: true,
      assimilateDisabled: false,
    })
    .withRouter()
    .mount({ attachTo: document.body })
  await flushPromises()
  await expandAssimilationPropertiesSection()
}

export function propertyAssimilateButton(propertyKey: string) {
  return propertyRowControl(propertyKey, assimilateButtonSelector)
}

export function propertySkipButton(propertyKey: string) {
  return propertyRowControl(propertyKey, skipButtonSelector)
}

export function propertyReviveButton(propertyKey: string) {
  return propertyRowControl(propertyKey, reviveButtonSelector)
}

export function propertyReturnToSequenceButton(propertyKey: string) {
  return propertyRowControl(propertyKey, returnToSequenceButtonSelector)
}

export function propertyRemoveFromRecallButton(propertyKey: string) {
  return propertyRowControl(propertyKey, removeFromRecallButtonSelector)
}

export async function clickPropertySkip(propertyKey: string) {
  propertySkipButton(propertyKey).click()
  await flushPromises()
}

export async function clickPropertyRevive(propertyKey: string) {
  propertyReviveButton(propertyKey).click()
  await flushPromises()
}

export async function clickPropertyReturnToSequence(propertyKey: string) {
  propertyReturnToSequenceButton(propertyKey).click()
  await flushPromises()
}

export async function clickPropertyRemoveFromRecall(propertyKey: string) {
  propertyRemoveFromRecallButton(propertyKey).click()
  await flushPromises()
}

export { assimilationPropertyRow, clickPropertyAssimilate }
