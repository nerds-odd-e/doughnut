import { flushPromises } from "@vue/test-utils"
import NoteUndoButton from "@/components/toolbars/NoteUndoButton.vue"
import NoteEditingHistory from "@/store/NoteEditingHistory"
import createNoteStorage from "@/store/createNoteStorage"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import { beforeEach, vi } from "vitest"
import { screen } from "@testing-library/vue"

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
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo delete note")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Confirm Undo")).toBeInTheDocument()
        expect(
          screen.getByText(/Are you sure you want to undo deleting "My Note"\?/)
        ).toBeInTheDocument()
      })

      it("shows confirmation dialog with note title and diff for edit title", async () => {
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
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo edit title")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Confirm Undo")).toBeInTheDocument()
        expect(
          screen.getByText(
            /Are you sure you want to undo editing the title of "Test Note"\?/
          )
        ).toBeInTheDocument()
        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("shows confirmation dialog with note title and diff for edit details", async () => {
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
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo edit details")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Confirm Undo")).toBeInTheDocument()
        expect(
          screen.getByText(
            /Are you sure you want to undo editing the details of "Details Note"\?/
          )
        ).toBeInTheDocument()
        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("shows diff view for edit details with long content", async () => {
        const noteRealm = makeMe.aNoteRealm
          .topicConstructor("Details Note")
          .please()
        const storageAccessor = useStorageAccessor()
        storageAccessor.value.refreshNoteRealm(noteRealm)
        const longDetails = "A".repeat(150)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit details",
          longDetails
        )
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo edit details")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("strips HTML tags from details content in diff view", async () => {
        const noteRealm = makeMe.aNoteRealm
          .topicConstructor("Details Note")
          .please()
        const storageAccessor = useStorageAccessor()
        storageAccessor.value.refreshNoteRealm(noteRealm)
        noteEditingHistory.addEditingToUndoHistory(
          noteRealm.id,
          "edit details",
          "<p>Old <strong>Details</strong> with <em>HTML</em></p>"
        )
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo edit details")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Will restore to")).toBeInTheDocument()
        // HTML should be stripped in the diff view
        const diffContent = screen.getByText("Will restore to").parentElement
        expect(diffContent?.textContent).toContain("Old Details with HTML")
      })
    })

    describe("when note is not in cache", () => {
      it("shows confirmation dialog with note id for delete note", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.deleteNote(note.id)
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo delete note")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Confirm Undo")).toBeInTheDocument()
        expect(
          screen.getByText(
            new RegExp(
              `Are you sure you want to undo deleting note id: ${note.id}\\?`
            )
          )
        ).toBeInTheDocument()
      })

      it("shows confirmation dialog with note id and diff for edit title", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.addEditingToUndoHistory(
          note.id,
          "edit title",
          "Old Title"
        )
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo edit title")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Confirm Undo")).toBeInTheDocument()
        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })

      it("shows confirmation dialog with note id and diff for edit details", async () => {
        const note = makeMe.aNote.please()
        noteEditingHistory.addEditingToUndoHistory(
          note.id,
          "edit details",
          "Old Details"
        )
        helper.component(NoteUndoButton).render()

        const undoButton = screen.getByTitle("undo edit details")
        await undoButton.click()
        await flushPromises()

        expect(screen.getByText("Confirm Undo")).toBeInTheDocument()
        expect(screen.getByText("Current")).toBeInTheDocument()
        expect(screen.getByText("Will restore to")).toBeInTheDocument()
      })
    })

    it("calls undo when confirmation is accepted for delete note", async () => {
      const note = makeMe.aNote.please()
      const noteRealm = makeMe.aNoteRealm.please()
      noteEditingHistory.deleteNote(note.id)
      mockSdkService("undoDeleteNote", noteRealm)
      helper.component(NoteUndoButton).render()

      const undoButton = screen.getByTitle("undo delete note")
      await undoButton.click()
      await flushPromises()

      // Accept confirmation
      const okButton = screen.getByRole("button", { name: "OK" })
      await okButton.click()
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
      helper.component(NoteUndoButton).render()

      const undoButton = screen.getByTitle("undo edit title")
      await undoButton.click()
      await flushPromises()

      // Accept confirmation
      const okButton = screen.getByRole("button", { name: "OK" })
      await okButton.click()
      await flushPromises()

      expect(mockedPush).toHaveBeenCalledWith({
        name: "noteShow",
        params: { noteId: noteRealm.id },
      })
    })

    it("does not call undo when confirmation is cancelled", async () => {
      const note = makeMe.aNote.please()
      noteEditingHistory.deleteNote(note.id)
      helper.component(NoteUndoButton).render()

      const undoButton = screen.getByTitle("undo delete note")
      await undoButton.click()
      await flushPromises()

      // Cancel confirmation
      const cancelButton = screen.getByRole("button", { name: "Cancel" })
      await cancelButton.click()
      await flushPromises()

      expect(mockedPush).not.toHaveBeenCalled()
    })
  })
})
