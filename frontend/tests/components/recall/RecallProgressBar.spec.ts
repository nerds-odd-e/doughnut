import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import type { LearningSessionLite } from "@generated/doughnut-backend-api"
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
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

beforeEach(() => {
  vi.restoreAllMocks()
})

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

const openLearningSessionList = async () => {
  await wrapper!.find('[data-test="learning-session-actions"]').trigger("click")
}

const findListEntry = (text: string) =>
  [
    ...document.body.querySelectorAll(
      '[data-test="learning-session-action-entry"]'
    ),
  ].find((entry) => entry.textContent?.includes(text))

const expectListDialogVisible = () => {
  expect(
    document.body.querySelector('[data-test="learning-session-list-dialog"]')
  ).toBeTruthy()
}

const expectListDialogHidden = () => {
  expect(
    document.body.querySelector('[data-test="learning-session-list-dialog"]')
  ).toBeFalsy()
}

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

  it("opens session list when icon is clicked with one potential session", async () => {
    mountBar({
      potentialLearningSessions: [spanishPotentialSession()],
    })
    await openLearningSessionList()
    expectListDialogVisible()
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeFalsy()
  })

  it("opens session list when icon is clicked with one awaiting session", async () => {
    mountBar({
      awaitingReportSessions: [spanishLearningSession()],
    })
    await openLearningSessionList()
    expectListDialogVisible()
    expect(
      document.body.querySelector('[data-test="learning-session-request"]')
    ).toBeFalsy()
  })

  it("opens request detail after picking from session list", async () => {
    mockSdkService(LearningSessionController, "request", {
      requestMarkdown: canonicalRequestMarkdown,
    })
    mountBar({
      potentialLearningSessions: [
        spanishPotentialSession(),
        kanjiPotentialSession(),
      ],
    })
    await openLearningSessionList()
    expectListDialogVisible()
    const kanjiEntry = findListEntry("Kanji")
    expect(kanjiEntry).toBeTruthy()
    ;(kanjiEntry as HTMLButtonElement).click()
    await wrapper!.vm.$nextTick()
    expectListDialogHidden()
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-submit"]'
      )
    ).toBeFalsy()
  })

  it("opens record detail after picking Record report from session list", async () => {
    mountBar({
      awaitingReportSessions: [spanishLearningSession()],
    })
    await openLearningSessionList()
    const recordEntry = findListEntry("Record report")
    expect(recordEntry).toBeTruthy()
    ;(recordEntry as HTMLButtonElement).click()
    await wrapper!.vm.$nextTick()
    expectListDialogHidden()
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-dialog"]'
      )
    ).toBeTruthy()
    const request = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement | null
    expect(request?.value).toBe(canonicalRequestMarkdown)
    expect(
      document.body.querySelector(
        '[data-test="learning-session-awaiting-report"]'
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

  it("opens amend detail with request prefilled when Amend report is picked", async () => {
    mountBar({
      recordedSessions: [spanishLearningSession()],
    })
    await openLearningSessionList()
    const amendEntry = findListEntry("Amend report")
    expect(amendEntry).toBeTruthy()
    expect(amendEntry!.textContent).toContain("Spanish conversation")
    ;(amendEntry as HTMLButtonElement).click()
    await wrapper!.vm.$nextTick()
    expectListDialogHidden()
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
