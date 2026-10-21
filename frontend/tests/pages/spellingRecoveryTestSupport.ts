import { MemoryTrackerController } from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import {
  createMemoryTrackerLite,
  givenRecallQueue,
} from "./recallPageTestSupport"

export const input = (wrapper: VueWrapper) =>
  wrapper.find("input#memory_tracker-answer")
export const retry = (wrapper: VueWrapper) =>
  wrapper.find('[data-testid="retry-answer"]')
export const progress = (wrapper: VueWrapper) =>
  wrapper.find(".progress-bar").text()
const prompt = (id = 101) =>
  makeMe.aRecallPrompt
    .withId(id)
    .withSpellingStem(
      id === 101 ? "First spelling question" : "Next spelling question"
    )
    .please()
export const result = (correct: boolean) =>
  makeMe.anAnsweredQuestion
    .withId(101)
    .withMemoryTrackerId(1)
    .spelling()
    .withAnswer({ id: 201, correct, spellingAnswer: "sedition" })
    .please()

export const givenSpellingRecoveryQueue = () => {
  givenRecallQueue(
    createMemoryTrackerLite(1, true),
    createMemoryTrackerLite(2, true)
  )
  mockSdkService(
    MemoryTrackerController,
    "getRecallPrompt",
    prompt(102)
  ).mockResolvedValueOnce(wrapSdkResponse(prompt()))
  mockSdkService(
    MemoryTrackerController,
    "showMemoryTracker",
    makeMe.aMemoryTracker.please()
  )
  mockSdkService(MemoryTrackerController, "getThresholdExceeded", {
    thresholdExceeded: false,
  })
}

export const submit = async (wrapper: VueWrapper, text = "sedition") => {
  await input(wrapper).setValue(text)
  await wrapper.find("form").trigger("submit")
  await flushPromises()
}
