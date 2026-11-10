import NoteUnderQuestion from "@/components/recall/NoteUnderQuestion.vue"
import {
  notePropertyLocation,
  noteShowLocation,
} from "@/routes/noteShowLocation"
import helper, { mockShowNote, productionRouterAt } from "@tests/helpers"
import { flushPromises } from "@vue/test-utils"
import makeMe from "donut-test-fixtures/makeMe"
import { beforeEach, describe, it, expect } from "vitest"

beforeEach(() => mockShowNote())

describe("NoteUnderQuestion", () => {
  it("opens the full note separately from its reviewed property", async () => {
    const noteTopology = makeMe.aNote.title("Test Note").please().noteTopology
    const router = await productionRouterAt({ name: "root" })
    const wrapper = helper
      .component(NoteUnderQuestion)
      .withRealRouter(router)
      .withProps({ noteTopology, focusedPropertyKey: "a part of" })
      .mount()
    await wrapper.get('[data-testid="reviewed-property-link"]').trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject(
      notePropertyLocation(noteTopology.id, "a part of")
    )
    await wrapper
      .findAll("a")
      .find((a) => a.text() === "Test Note")!
      .trigger("click")
    await flushPromises()
    expect(router.currentRoute.value).toMatchObject(
      noteShowLocation(noteTopology.id)
    )
    wrapper.unmount()
  })
})
