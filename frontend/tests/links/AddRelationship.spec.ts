import AddRelationshipFinalize from "@/components/links/AddRelationshipFinalize.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("AddRelationshipFinalize", () => {
  it("going back", async () => {
    const note = makeMe.aNoteRealm.please()
    const wrapper = helper
      .component(AddRelationshipFinalize)
      .withCleanStorage()
      .withProps({
        note,
        targetNoteTopology: note.note.noteTopology,
      })
      .mount()
    await wrapper.find(".go-back-button").trigger("click")
    expect(wrapper.emitted().goBack).toHaveLength(1)
  })
})
