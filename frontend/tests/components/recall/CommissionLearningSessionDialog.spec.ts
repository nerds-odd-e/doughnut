import CommissionLearningSessionDialog from "@/components/recall/CommissionLearningSessionDialog.vue"
import { LearningSessionController } from "@generated/donut-backend-api/sdk.gen"
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
    const wrapper = mountDialog()
    await flushPromises()
    return wrapper
  }

  it("fetches request markdown with notebook id on open", async () => {
    const requestSpy = mockSdkService(LearningSessionController, "request", {
      requestMarkdown: canonicalRequestMarkdown,
    })
    mountDialog()
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
    mountDialog()
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

  it("records report with notebook id in API body and shows recorded items", async () => {
    const recordSpy = mockSdkService(LearningSessionController, "record", {
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [
        { noteTitle: "Hola", grade: 4, memoryTrackerId: 11 },
        { noteTitle: "Gracias", grade: 1, memoryTrackerId: 12 },
      ],
      rejectedEntries: [],
    })
    const wrapper = await openRequestMode()
    const reportMarkdown = "# Learning Session Report\n\nHola: 4\nGracias: 1\n"

    const reportTextarea = document.body.querySelector(
      '[data-test="learning-session-report"]'
    ) as HTMLTextAreaElement
    reportTextarea.value = reportMarkdown
    reportTextarea.dispatchEvent(new Event("input"))

    await clickRecordReportSubmit()

    expect(recordSpy).toHaveBeenCalledWith({
      body: {
        notebookId: 42,
        reportMarkdown,
      },
      query: { timezone: expect.any(String) },
    })
    expect(
      document.body.querySelector(
        '[data-test="learning-session-recorded-items"]'
      )
    ).toBeTruthy()
    expect(
      document.body.querySelector(
        '[data-test="learning-session-recorded-items"]'
      )?.textContent
    ).toContain("Hola: 4")
    expect(wrapper.emitted("recorded")).toBeTruthy()
  })

  it("keeps report textarea when record fails and shows rejection warning on partial success", async () => {
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

    document.body.querySelectorAll("dialog").forEach((el) => el.remove())
    mockSdkService(LearningSessionController, "record", {
      recordedAt: "1989-01-02T09:00:00Z",
      recordedItems: [{ noteTitle: "Hola", grade: 4, memoryTrackerId: 11 }],
      rejectedEntries: [
        {
          line: "Unknown: 3",
          reason: "Note title not found in notebook.",
        },
      ],
    })
    await openRequestMode()
    await clickRecordReportSubmit()

    expect(
      document.body.querySelector(
        '[data-test="learning-session-report-rejections"]'
      )?.textContent
    ).toContain("Unknown: 3")
  })
})
