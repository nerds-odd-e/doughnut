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

describe("RecallProgressBar potential learning sessions", () => {
  it("renders nothing when there are no potential sessions", () => {
    const wrapper = mountBar()
    expect(
      wrapper.find('[data-test="potential-learning-session"]').exists()
    ).toBe(false)
  })

  it("opens commission dialog when Commission is clicked", async () => {
    const wrapper = mountBar({
      potentialLearningSessions: [
        {
          notebookId: 1,
          notebookName: "Spanish conversation",
        },
      ],
    })
    await wrapper
      .find('[data-test="commission-learning-session"]')
      .trigger("click")
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeTruthy()
  })
})

describe("RecallProgressBar awaiting report sessions", () => {
  it("renders awaiting-report strip with Record report button", () => {
    const wrapper = mountBar({
      awaitingReportSessions: [spanishLearningSession()],
    })
    const row = wrapper.find('[data-test="awaiting-report-learning-session"]')
    expect(row.exists()).toBe(true)
    expect(row.text()).toContain(
      `1 learning session awaiting the tutor's report for notebook "Spanish conversation"`
    )
    expect(
      row.find('[data-test="record-learning-session-report"]').exists()
    ).toBe(true)
  })

  it("opens record dialog with request prefilled when Record report is clicked", async () => {
    const wrapper = mountBar({
      awaitingReportSessions: [spanishLearningSession()],
    })
    await wrapper
      .find('[data-test="awaiting-report-learning-session"]')
      .find('[data-test="record-learning-session-report"]')
      .trigger("click")
    const request = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement | null
    expect(request).toBeTruthy()
    expect(request?.value).toBe(canonicalRequestMarkdown)
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
