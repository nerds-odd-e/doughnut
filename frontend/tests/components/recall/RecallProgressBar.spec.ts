import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import type { LearningSessionLite } from "@generated/doughnut-backend-api"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"

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

const mountBar = (props?: {
  potentialLearningSessions?: PotentialLearningSession[]
  awaitingReportSessions?: LearningSessionLite[]
  recordedSessions?: LearningSessionLite[]
}) =>
  helper
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
    .mount()

describe("RecallProgressBar learning session actions", () => {
  it("hides the session-actions icon when there are no actionable sessions", () => {
    const wrapper = mountBar()
    expect(
      wrapper.find('[data-test="learning-session-actions"]').exists()
    ).toBe(false)
  })

  it("shows badge count for potential and awaiting sessions", () => {
    const wrapper = mountBar({
      potentialLearningSessions: [spanishPotentialSession()],
      awaitingReportSessions: [spanishLearningSession()],
    })
    const badge = wrapper.find('[data-test="learning-session-actions-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe("2")
  })

  it("opens commission dialog directly when only one potential session exists", async () => {
    const wrapper = mountBar({
      potentialLearningSessions: [spanishPotentialSession()],
    })
    await wrapper
      .find('[data-test="learning-session-actions"]')
      .trigger("click")
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-actions-picker"]'
      )
    ).toBeFalsy()
  })

  it("opens record dialog directly when only one awaiting session exists", async () => {
    const wrapper = mountBar({
      awaitingReportSessions: [spanishLearningSession()],
    })
    await wrapper
      .find('[data-test="learning-session-actions"]')
      .trigger("click")
    const request = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement | null
    expect(request).toBeTruthy()
    expect(request?.value).toBe(canonicalRequestMarkdown)
    expect(
      document.body.querySelector(
        '[data-test="learning-session-actions-picker"]'
      )
    ).toBeFalsy()
  })

  it("opens picker then commission dialog when multiple actionable sessions exist", async () => {
    const wrapper = mountBar({
      potentialLearningSessions: [
        spanishPotentialSession(),
        kanjiPotentialSession(),
      ],
    })
    await wrapper
      .find('[data-test="learning-session-actions"]')
      .trigger("click")
    expect(
      wrapper.find('[data-test="learning-session-actions-picker"]').exists()
    ).toBe(true)
    const kanjiEntry = wrapper
      .findAll('[data-test="learning-session-action-entry"]')
      .find((entry) => entry.text().includes("Kanji"))
    expect(kanjiEntry).toBeTruthy()
    await kanjiEntry!.trigger("click")
    expect(
      wrapper.find('[data-test="learning-session-actions-picker"]').exists()
    ).toBe(false)
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeTruthy()
  })
})

describe("RecallProgressBar recorded sessions", () => {
  it("renders recorded-session strip with Amend report button", () => {
    const wrapper = mountBar({
      recordedSessions: [spanishLearningSession()],
    })
    const row = wrapper.find('[data-test="recorded-learning-session"]')
    expect(row.exists()).toBe(true)
    expect(row.text()).toContain(
      '1 recorded learning session for notebook "Spanish conversation"'
    )
    expect(
      row.find('[data-test="amend-learning-session-report"]').exists()
    ).toBe(true)
  })

  it("opens amend dialog with request prefilled when Amend report is clicked", async () => {
    const wrapper = mountBar({
      recordedSessions: [spanishLearningSession()],
    })
    await wrapper
      .find('[data-test="recorded-learning-session"]')
      .find('[data-test="amend-learning-session-report"]')
      .trigger("click")
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
