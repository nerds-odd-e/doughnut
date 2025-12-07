import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import NoteMoreOptionsDialog from "@/components/notes/accessory/NoteMoreOptionsDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { mockSdkService } from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"

it("displays menu items when dropdown is open", async () => {
  const noteRealm = makeMe.aNoteRealm.titleConstructor("Dummy Title").please()
  const mockNoteInfo: NoteInfo = {
    note: noteRealm,
    recallSetting: {
      level: 0,
      rememberSpelling: false,
      skipMemoryTracking: false,
    },
    memoryTrackers: [],
    createdAt: "",
  }
  mockSdkService("getNoteInfo", mockNoteInfo)

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

  // Check if menu items exist in the dialog (by title attribute for toolbar buttons)
  expect(wrapper.find('button[title="Questions for the note"]').exists()).toBe(
    true
  )
})
