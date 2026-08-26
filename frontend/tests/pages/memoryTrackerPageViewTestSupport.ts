import type {
  MemoryTracker,
  RecallPromptHistoryItem,
  RecallLog,
  RecallHistoryItem,
} from "@generated/doughnut-backend-api"
import { MemoryTrackerController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
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
  makeMe.aRecallPromptHistoryItem
    .withQuestionStem("Unanswered question")
    .please()

export const answeredRecallPrompt = () =>
  makeMe.aRecallPromptHistoryItem
    .withQuestionStem("Answered question")
    .withAnswer({
      id: 1,
      choiceIndex: 0,
      correct: true,
    })
    .withAnswerTime(new Date().toISOString())
    .please()

export const contestedRecallPrompt = () =>
  makeMe.aRecallPromptHistoryItem
    .withQuestionStem("Contested question")
    .withIsContested(true)
    .please()

export function historyFromPrompts(
  recallPrompts: RecallPromptHistoryItem[]
): RecallHistoryItem[] {
  return recallPrompts.map((recallPrompt) => ({ recallPrompt }))
}

export function historyFromLogs(recallLogs: RecallLog[]): RecallHistoryItem[] {
  return recallLogs.map((recallLog) => ({ recallLog }))
}

export type MountMemoryTrackerPageViewProps = {
  recallHistory?: RecallHistoryItem[]
  memoryTracker?: MemoryTracker
  memoryTrackerId?: number
}

export function mockMemoryTrackerPageViewDefaults() {
  mockSdkService(
    MemoryTrackerController,
    "removeFromRepeating",
    makeMe.aMemoryTracker.please()
  )
}

export function mountMemoryTrackerPageView(
  {
    recallHistory = [],
    memoryTracker = defaultMemoryTracker(),
    memoryTrackerId = defaultMemoryTrackerId,
  }: MountMemoryTrackerPageViewProps,
  options?: { attachToBody?: boolean }
) {
  return helper
    .component(MemoryTrackerPageView)
    .withRouter()
    .withProps({
      recallHistory,
      memoryTracker,
      memoryTrackerId,
    })
    .mount(options?.attachToBody ? { attachTo: document.body } : undefined)
}

export async function mountMemoryTrackerPageViewReady(
  props: MountMemoryTrackerPageViewProps,
  options?: { attachToBody?: boolean }
) {
  const wrapper = mountMemoryTrackerPageView(props, options)
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
  return makeMe.aRecallPromptHistoryItem
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

export function noteUnderQuestionSections(wrapper: VueWrapper) {
  return wrapper.findAll(".note-under-question")
}

export function skippedMemoryTracker() {
  return makeMe.aMemoryTracker.removedFromTracking(true).please()
}

export const spellingDetailsNotNeeded =
  "This is a spelling question. Details are not needed."
