import { NoteController } from "@generated/doughnut-backend-api/sdk.gen"
import AssimilationSettings from "@/components/recall/AssimilationSettings.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import {
  assimilationPropertyRow,
  clickPropertyAssimilate,
  expandAssimilationPropertiesSection,
  noteWithAssimilationProperties,
} from "./assimilationPropertyTestSupport"
import { assimilateButtonSelector } from "./assimilationPanelTestSupport"
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
  return assimilationPropertyRow(propertyKey).querySelector(
    assimilateButtonSelector
  ) as HTMLInputElement
}

export function propertySkipRecallButton(propertyKey: string) {
  return assimilationPropertyRow(propertyKey).querySelector(
    'input[name="skip"]'
  ) as HTMLInputElement
}

export function propertyReviveButton(propertyKey: string) {
  return assimilationPropertyRow(propertyKey).querySelector(
    '[data-test="revive"]'
  ) as HTMLInputElement
}

export async function clickPropertySkipRecall(propertyKey: string) {
  propertySkipRecallButton(propertyKey).click()
  await flushPromises()
}

export async function clickPropertyRevive(propertyKey: string) {
  propertyReviveButton(propertyKey).click()
  await flushPromises()
}

export { assimilationPropertyRow, clickPropertyAssimilate }
