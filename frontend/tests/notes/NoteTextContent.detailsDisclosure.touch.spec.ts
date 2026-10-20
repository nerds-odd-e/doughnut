import makeMe from "donut-test-fixtures/makeMe"
import { afterEach, expect, it } from "vitest"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { mountNoteTextContent } from "./noteTextContentTestSupport"

let wrapper: VueWrapper
afterEach(() => wrapper?.unmount())
it("offers a 44px details control to a coarse pointer", async () => {
  wrapper = mountNoteTextContent(makeMe.aNote.please())
  await flushPromises()
  expect(window.matchMedia("(pointer: coarse)").matches).toBe(true)
  const toggle = wrapper.get('[data-testid="note-details-toggle"]')
    .element as HTMLElement
  expect(toggle.getBoundingClientRect().height).toBeGreaterThanOrEqual(44)
  await wrapper.get('[data-testid="note-details-toggle"]').trigger("click")
  expect(toggle.getAttribute("aria-expanded")).toBe("true")
})
