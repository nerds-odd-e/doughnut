import AddRelationshipFinalize from "@/components/links/AddRelationshipFinalize.vue"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper from "@tests/helpers"
import { describe, it, expect } from "vitest"

describe("AddRelationshipFinalize", () => {
  it("going back", async () => {
    const note = makeMe.aNoteRealm.please()
    const targetTopology = makeMe.aNote.please().noteTopology
    const wrapper = helper
      .component(AddRelationshipFinalize)
      .withCleanStorage()
      .withProps({
        note,
        targetNoteTopology: targetTopology,
      })
      .mount()
    await wrapper.find(".go-back-button").trigger("click")
    expect(wrapper.emitted().goBack).toHaveLength(1)
  })
})
