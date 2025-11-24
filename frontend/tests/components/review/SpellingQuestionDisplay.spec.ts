import { describe, it, expect, vi, beforeEach } from "vitest"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import SpellingQuestionComponent from "@/components/review/SpellingQuestionComponent.vue"
import { MemoryTrackerController } from "@generated/backend/sdk.gen"

describe("SpellingQuestionDisplay", () => {
  beforeEach(() => {
    vi.spyOn(MemoryTrackerController, "getSpellingQuestion").mockResolvedValue({
      data: { stem: "Spell the word 'cat'" } as never,
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  it("renders spelling question input form", async () => {
    const wrapper = helper
      .component(SpellingQuestionComponent)
      .withProps({ memoryTrackerId: 1 })
      .mount()

    await flushPromises()

    expect(
      wrapper.find("input[placeholder='put your answer here']").exists()
    ).toBe(true)
    expect(wrapper.find("input[type='submit']").exists()).toBe(true)
  })

  it("emits answer event when form is submitted", async () => {
    const wrapper = helper
      .component(SpellingQuestionComponent)
      .withProps({ memoryTrackerId: 1 })
      .mount()

    await flushPromises()

    // Set input value
    const input = wrapper.find("input[placeholder='put your answer here']")
    await input.setValue("cat")

    // Submit form
    await wrapper.find("form").trigger("submit")

    // Check emitted event
    const emitted = wrapper.emitted()
    expect(emitted.answer).toBeTruthy()
    expect(emitted.answer![0]).toEqual([{ spellingAnswer: "cat" }])
  })
})
