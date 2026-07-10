import type {
  MemoryTracker,
  RecallPromptHistoryItem,
} from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import MemoryTrackerPageView from "@/pages/MemoryTrackerPageView.vue"

export const defaultMemoryTrackerId = 1

export const deleteUnansweredButtonTitle =
  "delete all unanswered recall prompts"
export const reviveButtonTitle = "Revive this memory tracker"
export const removeFromRecallButtonTitle = "remove this note from recall"
export const skippedBannerText =
  "This memory tracker is currently skipped and will not appear in recall sessions."

export const defaultMemoryTracker = () => makeMe.aMemoryTracker.please()

export const unansweredRecallPrompt = () =>
  makeMe.aRecallPrompt.withQuestionStem("Unanswered question").please()

export const answeredRecallPrompt = () =>
  makeMe.aRecallPrompt
    .withQuestionStem("Answered question")
    .withAnswer({
      id: 1,
      choiceIndex: 0,
      correct: true,
    })
    .withAnswerTime(new Date().toISOString())
    .please()

export const contestedRecallPrompt = () =>
  makeMe.aRecallPrompt
    .withQuestionStem("Contested question")
    .withIsContested(true)
    .please()

export type MountMemoryTrackerPageViewProps = {
  recallPrompts: RecallPromptHistoryItem[]
  memoryTracker?: MemoryTracker
  memoryTrackerId?: number
}

export function mountMemoryTrackerPageView({
  recallPrompts,
  memoryTracker = defaultMemoryTracker(),
  memoryTrackerId = defaultMemoryTrackerId,
}: MountMemoryTrackerPageViewProps) {
  return helper
    .component(MemoryTrackerPageView)
    .withProps({
      recallPrompts,
      memoryTracker,
      memoryTrackerId,
    })
    .mount()
}

export async function mountMemoryTrackerPageViewReady(
  props: MountMemoryTrackerPageViewProps
) {
  const wrapper = mountMemoryTrackerPageView(props)
  await flushPromises()
  return wrapper
}

export function deleteUnansweredButton(wrapper: VueWrapper) {
  return wrapper.find(`button[title="${deleteUnansweredButtonTitle}"]`)
}

export function reviveButton(wrapper: VueWrapper) {
  return wrapper.find(`button[title="${reviveButtonTitle}"]`)
}

export function removeFromRecallButton(wrapper: VueWrapper) {
  return wrapper.find(`button[title="${removeFromRecallButtonTitle}"]`)
}

export function focusedPropertyIndicator(wrapper: VueWrapper) {
  return wrapper.find('[data-testid="focused-property-indicator"]')
}

export function mockDeleteUnansweredRecallPrompts() {
  return mockSdkService(
    MemoryTrackerController,
    "deleteUnansweredRecallPrompts",
    undefined
  )
}

export async function clickDeleteUnanswered(wrapper: VueWrapper) {
  await deleteUnansweredButton(wrapper).trigger("click")
  await flushPromises()
}

export function peekConfirmPopup() {
  return usePopups().popups.peek()
}

export async function resolveConfirmPopup(confirmed: boolean) {
  usePopups().popups.done(confirmed)
  await flushPromises()
}

export function recallPromptWithThinkingTime(thinkingTimeMs: number) {
  return makeMe.aRecallPrompt
    .withQuestionStem("Test question")
    .withChoices(["A", "B", "C"])
    .withAnswer({
      id: 1,
      choiceIndex: 0,
      correct: true,
      thinkingTimeMs,
    })
    .please()
}
