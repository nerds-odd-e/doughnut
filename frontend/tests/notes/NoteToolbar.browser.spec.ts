import NoteToolbar from "@/components/notes/core/NoteToolbar.vue"
import NoteMoreOptionsDialog from "@/components/notes/accessory/NoteMoreOptionsDialog.vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import { mockSdkService } from "@tests/helpers"
import type { NoteInfo } from "@generated/backend"
import { describe, it, expect, afterEach } from "vitest"
import { type VueWrapper, flushPromises } from "@vue/test-utils"

describe("NoteToolbar", () => {
  // biome-ignore lint/suspicious/noExplicitAny: wrapper for testing
  let wrapper: VueWrapper<any>

  afterEach(() => {
    wrapper?.unmount()
    document.body.innerHTML = ""
  })

  it("displays menu items when dropdown is open", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
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

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps({
        note: noteRealm.note,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    // Find the more options button by title
    const moreOptionsButton = wrapper.find('button[title="more options"]')

    // Simulate a click event on the button to open the dialog
    await moreOptionsButton.trigger("click")
    await flushPromises()

    // Check if the dialog component exists
    const dialog = wrapper.findComponent(NoteMoreOptionsDialog)
    expect(dialog.exists()).toBe(true)

    // Check if menu items exist in the dialog (by title attribute for toolbar buttons)
    expect(
      wrapper.find('button[title="Questions for the note"]').exists()
    ).toBe(true)
  })

  it("closes more options dialog when note id changes", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Dummy Title").please()
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

    wrapper = helper
      .component(NoteToolbar)
      .withRouter()
      .withCleanStorage()
      .withProps({
        note: noteRealm.note,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    // Find the more options button by title
    const moreOptionsButton = wrapper.find('button[title="more options"]')

    // Open the dialog
    await moreOptionsButton.trigger("click")
    await flushPromises()

    // Verify dialog is open
    let dialog = wrapper.findComponent(NoteMoreOptionsDialog)
    expect(dialog.exists()).toBe(true)

    // Change the note id
    const newNote = makeMe.aNoteRealm.title("New Note").please()
    await wrapper.setProps({
      note: newNote.note,
    })
    await flushPromises()

    // Verify dialog is closed
    dialog = wrapper.findComponent(NoteMoreOptionsDialog)
    expect(dialog.exists()).toBe(false)
  })
})
