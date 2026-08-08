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
      .withRouter()
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
    expect(row.find(".break-words").text()).toBe(
      '1 potential learning session to commission for notebook "Spanish conversation"'
    )
  })

  it("renders nothing when there are no potential sessions", () => {
    const wrapper = mountBar([])
    expect(
      wrapper.find('[data-test="potential-learning-session"]').exists()
    ).toBe(false)
  })

  it("renders one row per notebook with distinct titles", () => {
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        trackerIds: [11, 12],
      },
      {
        notebookId: 2,
        notebookName: "Kanji",
        trackerIds: [21],
      },
    ])
    const rows = wrapper.findAll('[data-test="potential-learning-session"]')
    expect(rows).toHaveLength(2)
    expect(rows[0]!.text()).toContain('"Spanish conversation"')
    expect(rows[1]!.text()).toContain('"Kanji"')
  })

  it("keeps the full long notebook title in the row text", () => {
    const longTitle =
      "Advanced Spanish conversation practice for intermediate learners who want to master subjunctive mood"
    const wrapper = mountBar([
      {
        notebookId: 3,
        notebookName: longTitle,
        trackerIds: [31],
      },
    ])
    const row = wrapper.find('[data-test="potential-learning-session"]')
    expect(row.find(".break-words").text()).toContain(`"${longTitle}"`)
    expect(row.find('[data-test="commission-learning-session"]').exists()).toBe(
      true
    )
  })

  it("opens commission dialog when Commission is clicked", async () => {
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        trackerIds: [11],
      },
    ])
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
  const mountBar = (
    awaitingReportSessions: {
      notebookId: number
      notebookName: string
      learningSessionId: number
      requestMarkdown: string
    }[]
  ) =>
    helper
      .component(RecallProgressBar)
      .withRouter()
      .withProps({
        finished: 0,
        toRepeatCount: 0,
        canMoveToEnd: false,
        currentIndex: 0,
        previousAnsweredQuestions: [],
        potentialLearningSessions: [],
        awaitingReportSessions,
      })
      .mount()

  it("renders awaiting-report strip with Record report button", () => {
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        learningSessionId: 42,
        requestMarkdown: "# Learning Session Request\n\n### Hola\n",
      },
    ])
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
    const requestMarkdown = "# Learning Session Request\n\n### Hola\n"
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        learningSessionId: 42,
        requestMarkdown,
      },
    ])
    await wrapper
      .find('[data-test="awaiting-report-learning-session"]')
      .find('[data-test="record-learning-session-report"]')
      .trigger("click")
    const request = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement | null
    expect(request).toBeTruthy()
    expect(request?.value).toBe(requestMarkdown)
  })
})

describe("RecallProgressBar recorded sessions", () => {
  const mountBar = (
    recordedSessions: {
      notebookId: number
      notebookName: string
      learningSessionId: number
      requestMarkdown: string
    }[]
  ) =>
    helper
      .component(RecallProgressBar)
      .withRouter()
      .withProps({
        finished: 0,
        toRepeatCount: 0,
        canMoveToEnd: false,
        currentIndex: 0,
        previousAnsweredQuestions: [],
        potentialLearningSessions: [],
        awaitingReportSessions: [],
        recordedSessions,
      })
      .mount()

  it("renders recorded-session strip with Amend report button", () => {
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        learningSessionId: 42,
        requestMarkdown: "# Learning Session Request\n\n### Hola\n",
      },
    ])
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
    const requestMarkdown = "# Learning Session Request\n\n### Hola\n"
    const wrapper = mountBar([
      {
        notebookId: 1,
        notebookName: "Spanish conversation",
        learningSessionId: 42,
        requestMarkdown,
      },
    ])
    await wrapper
      .find('[data-test="recorded-learning-session"]')
      .find('[data-test="amend-learning-session-report"]')
      .trigger("click")
    const request = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement | null
    expect(request).toBeTruthy()
    expect(request?.value).toBe(requestMarkdown)
    expect(
      document.body.querySelector('[data-test="learning-session-recorded"]')
    ).toBeTruthy()
  })
})
