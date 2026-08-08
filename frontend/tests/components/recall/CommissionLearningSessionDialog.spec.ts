import CommissionLearningSessionDialog from "@/components/recall/CommissionLearningSessionDialog.vue"
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import helper from "@tests/helpers"

const canonicalRequestMarkdown = `# Learning Session Request

Notebook: Spanish conversation

score from 0 to 5 per item:

### Hola

Expected learning content: Hello

- Learning status: not yet tutored

### Gracias

Expected learning content: Thank you

- Learning status: not yet tutored
`

describe("CommissionLearningSessionDialog", () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    document.body.querySelectorAll("dialog").forEach((el) => el.remove())
  })
  const mountDialog = () =>
    helper
      .component(CommissionLearningSessionDialog)
      .withRouter()
      .withProps({
        notebookId: 42,
        notebookName: "Spanish conversation",
      })
      .mount()

  it("commissions and shows request markdown with awaiting banner", async () => {
    const commissionSpy = mockSdkService(
      LearningSessionController,
      "commission",
      {
        learningSessionId: 7,
        requestMarkdown: canonicalRequestMarkdown,
        status: "AWAITING_REPORT",
      }
    )
    mountDialog()
    await flushPromises()

    const submit = document.body.querySelector(
      '[data-test="commission-learning-session-submit"]'
    ) as HTMLButtonElement
    expect(submit).toBeTruthy()
    submit.click()
    await flushPromises()

    expect(commissionSpy).toHaveBeenCalledWith({
      body: { notebookId: 42 },
      query: { timezone: expect.any(String) },
    })

    const textarea = document.body.querySelector(
      '[data-test="learning-session-request"]'
    ) as HTMLTextAreaElement
    expect(textarea).toBeTruthy()
    expect(textarea.value).toContain("# Learning Session Request")
    expect(textarea.value).toContain("### Hola")
    expect(textarea.value).toContain("score from 0 to 5 per item")

    expect(
      document.body.querySelector(
        '[data-test="learning-session-awaiting-report"]'
      )
    ).toBeTruthy()
  })

  it("keeps pre-commission CTA when commission fails", async () => {
    const commissionSpy = vi.spyOn(LearningSessionController, "commission")
    commissionSpy.mockResolvedValue(wrapSdkError("commission failed") as never)
    mountDialog()
    await flushPromises()

    const submit = document.body.querySelector(
      '[data-test="commission-learning-session-submit"]'
    ) as HTMLButtonElement
    expect(submit).toBeTruthy()
    submit.click()
    await flushPromises()

    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-submit"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector('[data-test="learning-session-request"]')
    ).toBeNull()
  })

  it("shows report textarea after commission and records report", async () => {
    mockSdkService(LearningSessionController, "commission", {
      learningSessionId: 7,
      requestMarkdown: canonicalRequestMarkdown,
      status: "AWAITING_REPORT",
    })
    const recordSpy = mockSdkService(LearningSessionController, "record", {
      status: "RECORDED",
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [
        { noteTitle: "Hola", score: 5, memoryTrackerId: 11 },
        { noteTitle: "Gracias", score: 1, memoryTrackerId: 12 },
      ],
      rejectedEntries: [],
    })
    mountDialog()
    await flushPromises()

    const submit = document.body.querySelector(
      '[data-test="commission-learning-session-submit"]'
    ) as HTMLButtonElement
    submit.click()
    await flushPromises()

    const reportTextarea = document.body.querySelector(
      '[data-test="learning-session-report"]'
    ) as HTMLTextAreaElement
    expect(reportTextarea).toBeTruthy()
    reportTextarea.value = "# Learning Session Report\n\nHola: 5\nGracias: 1\n"
    reportTextarea.dispatchEvent(new Event("input"))

    const recordButton = document.body.querySelector(
      '[data-test="record-learning-session-report"]'
    ) as HTMLButtonElement
    recordButton.click()
    await flushPromises()

    expect(recordSpy).toHaveBeenCalledWith({
      body: {
        notebookId: 42,
        reportMarkdown: "# Learning Session Report\n\nHola: 5\nGracias: 1\n",
      },
      query: { timezone: expect.any(String) },
    })
    expect(
      document.body.querySelector('[data-test="learning-session-recorded"]')
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-awaiting-report"]'
      )
    ).toBeNull()
  })

  it("shows rejection warning alongside recorded banner on partial success", async () => {
    mockSdkService(LearningSessionController, "commission", {
      learningSessionId: 7,
      requestMarkdown: canonicalRequestMarkdown,
      status: "AWAITING_REPORT",
    })
    mockSdkService(LearningSessionController, "record", {
      status: "RECORDED",
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [{ noteTitle: "Hola", score: 5, memoryTrackerId: 11 }],
      rejectedEntries: [
        {
          line: "Unknown: 3",
          reason: "No session item matched this note title.",
        },
      ],
    })
    mountDialog()
    await flushPromises()

    const submit = document.body.querySelector(
      '[data-test="commission-learning-session-submit"]'
    ) as HTMLButtonElement
    submit.click()
    await flushPromises()

    const recordButton = document.body.querySelector(
      '[data-test="record-learning-session-report"]'
    ) as HTMLButtonElement
    recordButton.click()
    await flushPromises()

    expect(
      document.body.querySelector('[data-test="learning-session-recorded"]')
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-report-rejections"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-report-rejections"]'
      )?.textContent
    ).toContain("Unknown: 3")
  })

  it("mode amend shows recorded banner and emits recorded on success", async () => {
    const recordSpy = mockSdkService(LearningSessionController, "record", {
      status: "RECORDED",
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [{ noteTitle: "Gracias", score: 4, memoryTrackerId: 12 }],
      rejectedEntries: [],
    })
    const wrapper = helper
      .component(CommissionLearningSessionDialog)
      .withRouter()
      .withProps({
        notebookId: 42,
        notebookName: "Spanish conversation",
        mode: "amend",
        learningSessionId: 99,
        initialRequestMarkdown: canonicalRequestMarkdown,
      })
      .mount()
    await flushPromises()

    expect(
      document.body.querySelector('[data-test="learning-session-recorded"]')
    ).toBeTruthy()
    expect(
      document.body.querySelector('[data-test="learning-session-report"]')
    ).toBeTruthy()

    const reportTextarea = document.body.querySelector(
      '[data-test="learning-session-report"]'
    ) as HTMLTextAreaElement
    reportTextarea.value = "# Learning Session Report\n\nGracias: 4\n"
    reportTextarea.dispatchEvent(new Event("input"))

    const recordButton = document.body.querySelector(
      '[data-test="record-learning-session-report"]'
    ) as HTMLButtonElement
    recordButton.click()
    await flushPromises()

    expect(recordSpy).toHaveBeenCalledWith({
      body: {
        notebookId: 42,
        learningSessionId: 99,
        reportMarkdown: "# Learning Session Report\n\nGracias: 4\n",
      },
      query: { timezone: expect.any(String) },
    })
    expect(wrapper.emitted("recorded")).toBeTruthy()
  })

  it("keeps report textarea when record fails", async () => {
    mockSdkService(LearningSessionController, "commission", {
      learningSessionId: 7,
      requestMarkdown: canonicalRequestMarkdown,
      status: "AWAITING_REPORT",
    })
    const recordSpy = vi.spyOn(LearningSessionController, "record")
    recordSpy.mockResolvedValue(wrapSdkError("record failed") as never)
    mountDialog()
    await flushPromises()

    const submit = document.body.querySelector(
      '[data-test="commission-learning-session-submit"]'
    ) as HTMLButtonElement
    submit.click()
    await flushPromises()

    const recordButton = document.body.querySelector(
      '[data-test="record-learning-session-report"]'
    ) as HTMLButtonElement
    recordButton.click()
    await flushPromises()

    expect(
      document.body.querySelector(
        '[data-test="record-learning-session-report"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector('[data-test="learning-session-report"]')
    ).toBeTruthy()
  })
})
