import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import type {
  MemoryTracker,
  RecallHistoryItem,
} from "@generated/doughnut-backend-api"
import MemoryTrackerPage from "@/pages/MemoryTrackerPage.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import { vi } from "vitest"

export const memoryTrackerId = 123

export function mockMemoryTrackerPageApis(options?: {
  recallHistory?: RecallHistoryItem[]
  memoryTracker?: MemoryTracker
}) {
  const recallHistory = options?.recallHistory ?? [
    makeMe.aRecallHistoryItem
      .recallPrompt(makeMe.aRecallPromptHistoryItem.please())
      .please(),
  ]
  const memoryTracker = options?.memoryTracker ?? makeMe.aMemoryTracker.please()
  const getRecallHistorySpy = mockSdkService(
    MemoryTrackerController,
    "getRecallHistory",
    recallHistory
  )
  const showMemoryTrackerSpy = mockSdkService(
    MemoryTrackerController,
    "showMemoryTracker",
    memoryTracker
  )
  return {
    getRecallHistorySpy,
    showMemoryTrackerSpy,
    recallHistory,
    memoryTracker,
  }
}

export function mountMemoryTrackerPage(id = memoryTrackerId) {
  return helper
    .component(MemoryTrackerPage)
    .withProps({ memoryTrackerId: id })
    .mount()
}

export async function mountMemoryTrackerPageReady(options?: {
  recallHistory?: RecallHistoryItem[]
  memoryTracker?: MemoryTracker
  memoryTrackerId?: number
}) {
  mockMemoryTrackerPageApis(options)
  const wrapper = mountMemoryTrackerPage(options?.memoryTrackerId)
  await flushPromises()
  return wrapper
}

export function mockShowMemoryTrackerSequence(
  first: MemoryTracker,
  second: MemoryTracker
) {
  let fetchCount = 0
  return vi
    .spyOn(MemoryTrackerController, "showMemoryTracker")
    .mockImplementation(async () => {
      fetchCount += 1
      return wrapSdkResponse(fetchCount === 1 ? first : second) as never
    })
}
