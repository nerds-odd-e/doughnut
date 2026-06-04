import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import helper from "@tests/helpers"
import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect } from "vitest"

describe("NoteUnderQuestion", () => {
  it("shows focused property indicator when focusedPropertyKey is set", () => {
    const noteTopology = makeMe.aNote.title("Test Note").please().noteTopology

    const wrapper = helper
      .component(NoteUnderQuestion)
      .withProps({
        noteTopology,
        focusedPropertyKey: "a part of",
      })
      .mount()

    expect(
      wrapper.find('[data-testid="focused-property-indicator"]').exists()
    ).toBe(true)
    expect(wrapper.text()).toContain("Focused property: a part of")
  })

  it("omits focused property indicator when focusedPropertyKey is absent", () => {
    const noteTopology = makeMe.aNote.title("Test Note").please().noteTopology

    const wrapper = helper
      .component(NoteUnderQuestion)
      .withProps({ noteTopology })
      .mount()

    expect(
      wrapper.find('[data-testid="focused-property-indicator"]').exists()
    ).toBe(false)
    expect(wrapper.text()).not.toContain("Focused property:")
  })
})
