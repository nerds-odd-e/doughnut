import RecallProgressBar from "@/components/recall/RecallProgressBar.vue"
import helper from "@tests/helpers"
import { describe, expect, it } from "vitest"

describe("RecallProgressBar potential learning sessions", () => {
  const mountBar = (
    potentialLearningSessions: {
      notebookId: number
      notebookName: string
      trackerIds: number[]
    }[]
  ) =>
    helper
      .component(RecallProgressBar)
      .withProps({
        finished: 0,
        toRepeatCount: 0,
        canMoveToEnd: false,
        currentIndex: 0,
        previousAnsweredQuestions: [],
        potentialLearningSessions,
      })
      .mount()

  it("renders glossary copy for one notebook session", () => {
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        trackerIds: [11, 12],
      },
    ])
    const row = wrapper.find('[data-test="potential-learning-session"]')
    expect(row.exists()).toBe(true)
    expect(row.attributes("role")).toBe("status")
    expect(row.text()).toBe(
      '1 potential learning session to commission for notebook "Spanish conversation"'
    )
  })

  it("renders nothing when there are no potential sessions", () => {
    const wrapper = mountBar([])
    expect(
      wrapper.find('[data-test="potential-learning-session"]').exists()
    ).toBe(false)
  })
})
