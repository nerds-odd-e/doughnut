import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import NoteMoreOptionsDialog from "@/components/notes/accessory/NoteMoreOptionsDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

it("displays menu items when dropdown is open", async () => {
  const noteRealm = makeMe.aNoteRealm.topicConstructor("Dummy Title").please()
  const wrapper = helper
    .component(NoteToolbar)
    .withRouter()
    .withCleanStorage()
    .withProps({
      note: noteRealm.note,
    })
    .mount()

  // Find the more options button by title
  const moreOptionsButton = wrapper.find('button[title="more options"]')

  // Simulate a click event on the button to open the dialog
  await moreOptionsButton.trigger("click")

  // Check if the dialog component exists
  const dialog = wrapper.findComponent(NoteMoreOptionsDialog)
  expect(dialog.exists()).toBe(true)

  // Check if menu items exist in the dialog
  expect(dialog.text()).toContain("Questions for the note")
})
