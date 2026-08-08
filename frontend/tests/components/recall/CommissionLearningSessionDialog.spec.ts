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
})
