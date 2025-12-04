import { describe, expect, it } from "vitest"
import ContestableQuestion from "@/components/recall/ContestableQuestion.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("ContestableQuestion.vue", () => {
  const mountComponent = (recallPrompt = makeMe.aRecallPrompt.please()) => {
    return helper
      .component(ContestableQuestion)
      .withRouter()
      .withCleanStorage()
      .withProps({
        recallPrompt,
      })
      .mount()
  }

  it("renders recall prompt for multiple choice questions", () => {
    const wrapper = mountComponent()
    expect(
      wrapper.findComponent({ name: "RecallPromptComponent" }).exists()
    ).toBe(true)
  })

  it("emits answered event when question is answered", async () => {
    const wrapper = mountComponent()
    const answerResult = makeMe.anAnsweredQuestion.answerCorrect(true).please()

    await wrapper
      .findComponent({ name: "RecallPromptComponent" })
      .vm.$emit("answered", answerResult)

    const emitted = wrapper.emitted()
    expect(emitted.answered).toBeTruthy()
    expect(emitted.answered![0]).toEqual([answerResult])
  })
})
