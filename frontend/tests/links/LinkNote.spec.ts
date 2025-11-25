import LinkNoteFinalize from "@/components/links/LinkNoteFinalize.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"

describe("LinkNoteFinalize", () => {
  it("going back", async () => {
    const note = makeMe.aNoteRealm.please()
    const wrapper = helper
      .component(LinkNoteFinalize)
      .withStorageProps({
        note,
        targetNoteTopology: note.note.noteTopology,
      })
      .mount()
    await wrapper.find(".go-back-button").trigger("click")
    expect(wrapper.emitted().goBack).toHaveLength(1)
  })

  it.skip("creates link and moves note when moveUnder is true", async () => {
    const note = makeMe.aNoteRealm.please()
    const linkNoteFinalizeSpy = mockSdkService("linkNoteFinalize", undefined)
    const moveNoteSpy = mockSdkService("moveNote", undefined)
    const wrapper = helper
      .component(LinkNoteFinalize)
      .withStorageProps({
        note,
        targetNoteTopic: note.note.noteTopology,
      })
      .mount()

    await wrapper.find("input[type='checkbox']").setValue(true)
    await wrapper.find("button.btn-primary").trigger("click")

    expect(linkNoteFinalizeSpy).toHaveBeenCalled()
    expect(moveNoteSpy).toHaveBeenCalled()
  })
})
