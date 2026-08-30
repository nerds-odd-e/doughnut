import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import helper from "@tests/helpers"
import type { VueWrapper } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, it, expect } from "vitest"

function noteTitleLinkTo(wrapper: VueWrapper, title: string) {
  const link = wrapper
    .findAll("a.router-link")
    .find((anchor) => anchor.text().includes(title))
  expect(link, `Expected a title link for "${title}"`).toBeDefined()
  return JSON.parse(link!.attributes("to") ?? "{}")
}

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

  it("links the note title to noteProperty when focusedPropertyKey is set", () => {
    const noteTopology = makeMe.aNote.title("Test Note").please().noteTopology

    const wrapper = helper
      .component(NoteUnderQuestion)
      .withProps({
        noteTopology,
        focusedPropertyKey: "a part of",
      })
      .mount()

    expect(noteTitleLinkTo(wrapper, "Test Note")).toEqual(
      notePropertyLocation(noteTopology.id, "a part of")
    )
  })

  it("links the note title to noteShow when focusedPropertyKey is absent", () => {
    const noteTopology = makeMe.aNote.title("Test Note").please().noteTopology

    const wrapper = helper
      .component(NoteUnderQuestion)
      .withProps({ noteTopology })
      .mount()

    expect(noteTitleLinkTo(wrapper, "Test Note")).toEqual(
      noteShowLocation(noteTopology.id)
    )
  })
})
