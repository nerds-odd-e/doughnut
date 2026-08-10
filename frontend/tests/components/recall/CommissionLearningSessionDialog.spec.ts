import CommissionLearningSessionDialog from "@/components/recall/CommissionLearningSessionDialog.vue"
import { LearningSessionController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import helper from "@tests/helpers"

const canonicalRequestMarkdown = "# Learning Session Request\n\n### Hola\n"

describe("CommissionLearningSessionDialog", () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    document.body.querySelectorAll("dialog").forEach((el) => el.remove())
  })

  const mountDialog = (props?: Record<string, unknown>) =>
    helper
      .component(CommissionLearningSessionDialog)
      .withRouter()
      .withProps({
        notebookId: 42,
        notebookName: "Spanish conversation",
        ...props,
      })
      .mount()

  const clickRecordReportSubmit = async () => {
    const recordButton = document.body.querySelector(
      '[data-test="record-learning-session-report-submit"]'
    ) as HTMLButtonElement
    recordButton.click()
    await flushPromises()
  }

  const openRequestMode = async () => {
    mockSdkService(LearningSessionController, "request", {
      requestMarkdown: canonicalRequestMarkdown,
    })
    mountDialog({ mode: "request" })
    await flushPromises()
  }

  it("fetches request markdown with notebook id on open", async () => {
    const requestSpy = mockSdkService(LearningSessionController, "request", {
      requestMarkdown: canonicalRequestMarkdown,
    })
    mountDialog({ mode: "request" })
    await flushPromises()

    expect(requestSpy).toHaveBeenCalledWith({
      query: {
        notebookId: 42,
        timezone: expect.any(String),
      },
    })
    expect(
      (
        document.body.querySelector(
          '[data-test="learning-session-request"]'
        ) as HTMLTextAreaElement
      ).value
    ).toBe(canonicalRequestMarkdown)
    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-submit"]'
      )
    ).toBeNull()
    expect(
      document.body.querySelector('[data-test="learning-session-report"]')
    ).toBeTruthy()
  })

  it("does not show request when fetch fails", async () => {
    const requestSpy = vi.spyOn(LearningSessionController, "request")
    requestSpy.mockResolvedValue(wrapSdkError("request failed") as never)
    mountDialog({ mode: "request" })
    await flushPromises()

    expect(
      document.body.querySelector(
        '[data-test="learning-session-request-loading"]'
      )
    ).toBeNull()
    expect(
      document.body.querySelector('[data-test="learning-session-request"]')
    ).toBeNull()
  })

  it("records report with notebook id in API body", async () => {
    const recordSpy = mockSdkService(LearningSessionController, "record", {
      status: "RECORDED",
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [
        { noteTitle: "Hola", score: 5, memoryTrackerId: 11 },
        { noteTitle: "Gracias", score: 1, memoryTrackerId: 12 },
      ],
      rejectedEntries: [],
    })
    await openRequestMode()

    const reportTextarea = document.body.querySelector(
      '[data-test="learning-session-report"]'
    ) as HTMLTextAreaElement
    reportTextarea.value = "# Learning Session Report\n\nHola: 5\nGracias: 1\n"
    reportTextarea.dispatchEvent(new Event("input"))

    await clickRecordReportSubmit()

    expect(recordSpy).toHaveBeenCalledWith({
      body: {
        notebookId: 42,
        reportMarkdown: "# Learning Session Report\n\nHola: 5\nGracias: 1\n",
      },
      query: { timezone: expect.any(String) },
    })
  })

  it("shows rejection warning on partial success", async () => {
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
    await openRequestMode()

    await clickRecordReportSubmit()

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

  it("mode amend records with learningSessionId in API body and emits recorded", async () => {
    const recordSpy = mockSdkService(LearningSessionController, "record", {
      status: "RECORDED",
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [{ noteTitle: "Gracias", score: 4, memoryTrackerId: 12 }],
      rejectedEntries: [],
    })
    const wrapper = mountDialog({
      mode: "amend",
      learningSessionId: 99,
      initialRequestMarkdown: canonicalRequestMarkdown,
    })
    await flushPromises()

    const reportTextarea = document.body.querySelector(
      '[data-test="learning-session-report"]'
    ) as HTMLTextAreaElement
    reportTextarea.value = "# Learning Session Report\n\nGracias: 4\n"
    reportTextarea.dispatchEvent(new Event("input"))

    await clickRecordReportSubmit()

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
    const recordSpy = vi.spyOn(LearningSessionController, "record")
    recordSpy.mockResolvedValue(wrapSdkError("record failed") as never)
    await openRequestMode()

    await clickRecordReportSubmit()

    expect(
      document.body.querySelector(
        '[data-test="record-learning-session-report-submit"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector('[data-test="learning-session-report"]')
    ).toBeTruthy()
  })
})
