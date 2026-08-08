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

  const clickCommissionSubmit = async () => {
    const submit = document.body.querySelector(
      '[data-test="commission-learning-session-submit"]'
    ) as HTMLButtonElement
    submit.click()
    await flushPromises()
  }

  const commissionToAwaiting = async () => {
    mockSdkService(LearningSessionController, "commission", {
      learningSessionId: 7,
      requestMarkdown: canonicalRequestMarkdown,
      status: "AWAITING_REPORT",
    })
    mountDialog()
    await flushPromises()
    await clickCommissionSubmit()
  }

  it("commissions with notebook id in API body", async () => {
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
    await clickCommissionSubmit()

    expect(commissionSpy).toHaveBeenCalledWith({
      body: { notebookId: 42 },
      query: { timezone: expect.any(String) },
    })
  })

  it("keeps pre-commission CTA when commission fails", async () => {
    const commissionSpy = vi.spyOn(LearningSessionController, "commission")
    commissionSpy.mockResolvedValue(wrapSdkError("commission failed") as never)
    mountDialog()
    await flushPromises()
    await clickCommissionSubmit()

    expect(
      document.body.querySelector(
        '[data-test="commission-learning-session-submit"]'
      )
    ).toBeTruthy()
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
    await commissionToAwaiting()

    const reportTextarea = document.body.querySelector(
      '[data-test="learning-session-report"]'
    ) as HTMLTextAreaElement
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
    await commissionToAwaiting()

    const recordButton = document.body.querySelector(
      '[data-test="record-learning-session-report"]'
    ) as HTMLButtonElement
    recordButton.click()
    await flushPromises()

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
    const recordSpy = vi.spyOn(LearningSessionController, "record")
    recordSpy.mockResolvedValue(wrapSdkError("record failed") as never)
    await commissionToAwaiting()

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
