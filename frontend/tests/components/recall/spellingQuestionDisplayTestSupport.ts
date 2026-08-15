import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import SpellingQuestionDisplay from "@/components/recall/SpellingQuestionDisplay.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import { vi } from "vitest"

export const spellingAnswerInputSelector =
  "input[placeholder='put your answer here']"

export function mockSpellingQuestionServices() {
  mockSdkService(
    MemoryTrackerController,
    "getRecallPrompt",
    makeMe.aRecallPrompt.withSpellingStem("Spell the word").please()
  )
  mockSdkService(
    MemoryTrackerController,
    "showMemoryTracker",
    makeMe.aMemoryTracker.please()
  )
}

export function captureRequestAnimationFrame() {
  const callbacks: FrameRequestCallback[] = []
  vi.spyOn(window, "requestAnimationFrame").mockImplementation((callback) => {
    callbacks.push(callback)
    return 1
  })
  return callbacks
}

export function flushCapturedAnimationFrames(
  callbacks: FrameRequestCallback[],
  time = 0
) {
  const pending = callbacks.splice(0, callbacks.length)
  for (const callback of pending) {
    callback(time)
  }
}

export async function mountSpellingQuestionDisplay(
  props: { memoryTrackerId: number; nextIsSpelling?: boolean },
  options?: { attachTo?: HTMLElement; rafCallbacks?: FrameRequestCallback[] }
) {
  const wrapper = helper
    .component(SpellingQuestionDisplay)
    .withProps(props)
    .mount(options?.attachTo ? { attachTo: options.attachTo } : undefined)
  await flushPromises()
  if (options?.rafCallbacks) {
    flushCapturedAnimationFrames(options.rafCallbacks)
  }
  return wrapper
}

export async function submitSpellingAnswer(
  wrapper: VueWrapper,
  answer = "cat"
) {
  await wrapper.find(spellingAnswerInputSelector).setValue(answer)
  await wrapper.find("form").trigger("submit")
}
