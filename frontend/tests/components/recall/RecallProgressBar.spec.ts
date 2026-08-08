import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import type { LearningSessionLite } from "@generated/doughnut-backend-api"
import helper from "@tests/helpers"
import { afterEach, describe, expect, it } from "vitest"
import type { VueWrapper } from "@vue/test-utils"

const canonicalRequestMarkdown = "# Learning Session Request\n\n### Hola\n"

const spanishLearningSession = (
  requestMarkdown = canonicalRequestMarkdown
): LearningSessionLite => ({
  notebookId: 1,
  notebookName: "Spanish conversation",
  learningSessionId: 42,
  requestMarkdown,
})

const spanishPotentialSession = (): PotentialLearningSession => ({
  notebookId: 1,
  notebookName: "Spanish conversation",
})

const kanjiPotentialSession = (): PotentialLearningSession => ({
  notebookId: 2,
  notebookName: "Kanji",
})

let wrapper: VueWrapper | undefined

afterEach(() => {
  wrapper?.unmount()
  wrapper = undefined
})

const mountBar = (props?: {
  potentialLearningSessions?: PotentialLearningSession[]
  awaitingReportSessions?: LearningSessionLite[]
  recordedSessions?: LearningSessionLite[]
}) =>
  (wrapper = helper
    .component(RecallProgressBar)
    .withRouter()
    .withProps({
      finished: 0,
      toRepeatCount: 0,
      canMoveToEnd: false,
      currentIndex: 0,
      previousAnsweredQuestions: [],
      potentialLearningSessions: props?.potentialLearningSessions ?? [],
      awaitingReportSessions: props?.awaitingReportSessions ?? [],
      recordedSessions: props?.recordedSessions ?? [],
    })
    .mount())

const openLearningSessionActionsPicker = async () => {
  await wrapper!.find('[data-test="learning-session-actions"]').trigger("click")
}

const findPickerEntry = (text: string) =>
  [
    ...document.body.querySelectorAll(
      '[data-test="learning-session-action-entry"]'
    ),
  ].find((entry) => entry.textContent?.includes(text))

describe("RecallProgressBar learning session actions", () => {
  it("always shows the session-actions icon even with no sessions", () => {
    mountBar()
    expect(
      wrapper!.find('[data-test="learning-session-actions"]').exists()
    ).toBe(true)
    expect(
      wrapper!.find('[data-test="learning-session-actions-badge"]').exists()
    ).toBe(false)
  })

  it("shows badge count for potential, awaiting, and recorded sessions", () => {
    mountBar({
      potentialLearningSessions: [spanishPotentialSession()],
      awaitingReportSessions: [spanishLearningSession()],
      recordedSessions: [spanishLearningSession()],
    })
    const badge = wrapper!.find('[data-test="learning-session-actions-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe("3")
  })

  it("opens picker when only one potential session exists", async () => {
    mountBar({
      potentialLearningSessions: [spanishPotentialSession()],
    })
    await openLearningSessionActionsPicker()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-actions-picker"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeFalsy()
  })

  it("opens picker when only one awaiting session exists", async () => {
    mountBar({
      awaitingReportSessions: [spanishLearningSession()],
    })
    await openLearningSessionActionsPicker()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-actions-picker"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector('[data-test="learning-session-request"]')
    ).toBeFalsy()
  })

  it("opens picker then commission dialog when multiple actionable sessions exist", async () => {
    mountBar({
      potentialLearningSessions: [
        spanishPotentialSession(),
        kanjiPotentialSession(),
      ],
    })
    await openLearningSessionActionsPicker()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-actions-picker"]'
      )
    ).toBeTruthy()
    const kanjiEntry = findPickerEntry("Kanji")
    expect(kanjiEntry).toBeTruthy()
    ;(kanjiEntry as HTMLButtonElement).click()
    await wrapper!.vm.$nextTick()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-actions-picker"]'
      )
    ).toBeFalsy()
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeTruthy()
  })
})

describe("RecallProgressBar recorded sessions", () => {
  it("does not render recorded-session strips below the bar", () => {
    mountBar({
      recordedSessions: [spanishLearningSession()],
    })
    expect(
      wrapper!.find('[data-test="recorded-learning-session"]').exists()
    ).toBe(false)
  })

  it("opens amend dialog with request prefilled when Amend report is picked", async () => {
    mountBar({
      recordedSessions: [spanishLearningSession()],
    })
    await openLearningSessionActionsPicker()
    const amendEntry = findPickerEntry("Amend report")
    expect(amendEntry).toBeTruthy()
    expect(amendEntry!.textContent).toContain("Spanish conversation")
    ;(amendEntry as HTMLButtonElement).click()
    await wrapper!.vm.$nextTick()
    const request = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement | null
    expect(request).toBeTruthy()
    expect(request?.value).toBe(canonicalRequestMarkdown)
    expect(
      document.body.querySelector('[data-test="learning-session-recorded"]')
    ).toBeTruthy()
  })
})
