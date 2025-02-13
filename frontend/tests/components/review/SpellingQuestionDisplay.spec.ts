import { describe, it, expect, vi } from "vitest"
import { flushPromises } from "@vue/test-utils"
import helper from "@tests/helpers"
import SpellingQuestionComponent from "@/components/review/SpellingQuestionComponent.vue"
import makeMe from "@tests/fixtures/makeMe"

// Mock the API
vi.mock("@/managedApi/useLoadingApi", () => ({
  default: () => ({
    managedApi: {
      restMemoryTrackerController: {
        getSpellingQuestion: vi
          .fn()
          .mockResolvedValue({ stem: "Spell the word 'cat'" }),
      },
    },
  }),
}))

describe("SpellingQuestionDisplay", () => {
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
