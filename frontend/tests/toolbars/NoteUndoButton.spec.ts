import { flushPromises } from "@vue/test-utils"
import NoteUndoButton from "@/components/toolbars/NoteUndoButton.vue"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import usePopups from "@/components/commons/Popups/usePopups"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, vi } from "vitest"

const mockedPush = vi.fn()
vitest.mock("vue-router", () => ({
  useRoute: () => ({ path: "/" }),
  useRouter: () => ({
    push: mockedPush,
  }),
}))

describe("NoteUndoButton", () => {
  let noteEditingHistory: NoteEditingHistory

  beforeEach(() => {
    vi.clearAllMocks()
    noteEditingHistory = new NoteEditingHistory()
    const storageAccessor = useStorageAccessor()
    storageAccessor.value = createNoteStorage(noteEditingHistory)
    // Clear any existing popups
    const popups = usePopups().popups
    while (popups.peek()?.length) {
      popups.done(true)
    }
  })

  it("does not show when there is nothing to undo", () => {
    const wrapper = helper.component(NoteUndoButton).mount()
    expect(wrapper.find("button").exists()).toBe(false)
  })

  it("shows when there is something to undo", () => {
    const note = makeMe.aNote.please()
    noteEditingHistory.deleteNote(note.id)
    const wrapper = helper.component(NoteUndoButton).mount()
    expect(wrapper.find("button").exists()).toBe(true)
    expect(wrapper.find("button").attributes("title")).toBe("undo delete note")
  })

  describe("confirmation dialog", () => {
    describe("when note is in cache", () => {
      it("shows confirmation dialog with note title for delete note", async () => {
        const noteRealm = makeMe.aNoteRealm.topicConstructor("My Note").please()
        const storageAccessor = useStorageAccessor()
        storageAccessor.value.refreshNoteRealm(noteRealm)
        noteEditingHistory.deleteNote(noteRealm.id)
        const wrapper = helper.component(NoteUndoButton).mount()

        const undoButton = wrapper.find("button")
        await undoButton.trigger("click")
        await flushPromises()

        const popups = usePopups().popups.peek()
        expect(popups?.length).toBe(1)
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(
          'Are you sure you want to undo deleting "My Note"?'
        )

        // Clean up
        usePopups().popups.done(false)
        await flushPromises()
      })

      it("shows confirmation dialog with note title for edit title", async () => {
        const noteRealm = makeMe.aNoteRealm
          .topicConstructor("Test Note")
          .please()
        const storageAccessor = useStorageAccessor()
        storageAccessor.value.refreshNoteRealm(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit title",
          "Old Title"
        )
        const wrapper = helper.component(NoteUndoButton).mount()

        const undoButton = wrapper.find("button")
        await undoButton.trigger("click")
        await flushPromises()

        const popups = usePopups().popups.peek()
        expect(popups?.length).toBe(1)
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(
          'Are you sure you want to undo editing the title of "Test Note"?'
        )

        // Clean up
        usePopups().popups.done(false)
        await flushPromises()
      })

      it("shows confirmation dialog with note title for edit details", async () => {
        const noteRealm = makeMe.aNoteRealm
          .topicConstructor("Details Note")
          .please()
        const storageAccessor = useStorageAccessor()
        storageAccessor.value.refreshNoteRealm(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit details",
          "Old Details"
        )
        const wrapper = helper.component(NoteUndoButton).mount()

        const undoButton = wrapper.find("button")
        await undoButton.trigger("click")
        await flushPromises()

        const popups = usePopups().popups.peek()
        expect(popups?.length).toBe(1)
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(
          'Are you sure you want to undo editing the details of "Details Note"?'
        )

        // Clean up
        usePopups().popups.done(false)
        await flushPromises()
      })
    })

    describe("when note is not in cache", () => {
      it("shows confirmation dialog with note id for delete note", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.deleteNote(note.id)
        const wrapper = helper.component(NoteUndoButton).mount()

        const undoButton = wrapper.find("button")
        await undoButton.trigger("click")
        await flushPromises()

        const popups = usePopups().popups.peek()
        expect(popups?.length).toBe(1)
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(
          `Are you sure you want to undo deleting note id: ${note.id}?`
        )

        // Clean up
        usePopups().popups.done(false)
        await flushPromises()
      })

      it("shows confirmation dialog with note id for edit title", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.addEditingToUndoHistory(
          note.id,
          "edit title",
          "Old Title"
        )
        const wrapper = helper.component(NoteUndoButton).mount()

        const undoButton = wrapper.find("button")
        await undoButton.trigger("click")
        await flushPromises()

        const popups = usePopups().popups.peek()
        expect(popups?.length).toBe(1)
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(
          `Are you sure you want to undo editing the title of note id: ${note.id}?`
        )

        // Clean up
        usePopups().popups.done(false)
        await flushPromises()
      })

      it("shows confirmation dialog with note id for edit details", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.addEditingToUndoHistory(
          note.id,
          "edit details",
          "Old Details"
        )
        const wrapper = helper.component(NoteUndoButton).mount()

        const undoButton = wrapper.find("button")
        await undoButton.trigger("click")
        await flushPromises()

        const popups = usePopups().popups.peek()
        expect(popups?.length).toBe(1)
        expect(popups?.[0]?.type).toBe("confirm")
        expect(popups?.[0]?.message).toBe(
          `Are you sure you want to undo editing the details of note id: ${note.id}?`
        )

        // Clean up
        usePopups().popups.done(false)
        await flushPromises()
      })
    })

    it("calls undo when confirmation is accepted for delete note", async () => {
      const note = makeMe.aNote.please()
      const noteRealm = makeMe.aNoteRealm.please()
      noteEditingHistory.deleteNote(note.id)
      mockSdkService("undoDeleteNote", noteRealm)
      const wrapper = helper.component(NoteUndoButton).mount()

      const undoButton = wrapper.find("button")
      await undoButton.trigger("click")
      await flushPromises()

      // Accept confirmation
      usePopups().popups.done(true)
      await flushPromises()

      expect(mockedPush).toHaveBeenCalledWith({
        name: "noteShow",
        params: { noteId: noteRealm.id },
      })
    })

    it("calls undo when confirmation is accepted for edit title", async () => {
      const note = makeMe.aNote.please()
      const noteRealm = makeMe.aNoteRealm.please()
      noteEditingHistory.addEditingToUndoHistory(
        note.id,
        "edit title",
        "Old Title"
      )
      mockSdkService("updateNoteTitle", noteRealm)
      const wrapper = helper.component(NoteUndoButton).mount()

      const undoButton = wrapper.find("button")
      await undoButton.trigger("click")
      await flushPromises()

      // Accept confirmation
      usePopups().popups.done(true)
      await flushPromises()

      expect(mockedPush).toHaveBeenCalledWith({
        name: "noteShow",
        params: { noteId: noteRealm.id },
      })
    })

    it("does not call undo when confirmation is cancelled", async () => {
      const note = makeMe.aNote.please()
      noteEditingHistory.deleteNote(note.id)
      const wrapper = helper.component(NoteUndoButton).mount()

      const undoButton = wrapper.find("button")
      await undoButton.trigger("click")
      await flushPromises()

      // Cancel confirmation
      usePopups().popups.done(false)
      await flushPromises()

      expect(mockedPush).not.toHaveBeenCalled()
    })
  })
})
