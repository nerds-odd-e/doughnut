import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import type { PotentialLearningSession } from "@/composables/useRecallData"
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import helper, { mockSdkService } from "@tests/helpers"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { VueWrapper } from "@vue/test-utils"

const canonicalRequestMarkdown = "# Learning Session Request\n\n### Hola\n"

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

  it("shows badge count for potential sessions only", () => {
    mountBar({
      potentialLearningSessions: [
        spanishPotentialSession(),
        kanjiPotentialSession(),
      ],
    })
    const badge = wrapper!.find('[data-test="learning-session-actions-badge"]')
    expect(badge.exists()).toBe(true)
    expect(badge.text()).toBe("2")
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
})
