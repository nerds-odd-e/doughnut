import {
  NoteController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { describe, it, expect, vi } from "vitest"
import {
  clickDialogCancel,
  clickDialogDiscard,
  clickDialogOk,
  clickUndoButton,
  expectConfirmUndoHidden,
  expectConfirmUndoVisible,
  expectNoteTitleHidden,
  expectNoteTitleVisible,
  mockedPush,
  noteEditingHistory,
  refreshNoteRealms,
  renderNoteUndoButton,
  setupNoteUndoButtonTests,
  setupTwoCachedNotes,
} from "./noteUndoButtonTestSupport"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  const { noteUndoButtonRouterMockExports } = await import(
    "./noteUndoButtonMocks"
  )
  return noteUndoButtonRouterMockExports(actual)
})

setupNoteUndoButtonTests()

describe("NoteUndoButton actions", () => {
  it.each([
    {
      label: "trash note",
      setup: () => {
        const noteRealm = makeMe.aNoteRealm.title("Original title").please()
        noteEditingHistory.trashNote(noteRealm.id, "Original title", null)
        mockSdkService(NoteController, "undoTrashNote", noteRealm)
        return { noteRealm, undoTitle: "undo trash note" }
      },
    },
    {
      label: "edit title",
      setup: () => {
        const note = makeMe.aNote.please()
        const noteRealm = makeMe.aNoteRealm.please()
        noteEditingHistory.addEditingToUndoHistory(
          note.id,
          "edit title",
          "Old Title"
        )
        mockSdkService(TextContentController, "updateNoteTitle", noteRealm)
        return { noteRealm, undoTitle: "undo edit title" }
      },
    },
    {
      label: "edit content without prior content",
      setup: () => {
        const note = makeMe.aNote.please()
        const noteRealm = makeMe.aNoteRealm.please()
        noteEditingHistory.addEditingToUndoHistory(
          note.id,
          "edit content",
          undefined
        )
        mockSdkService(TextContentController, "updateNoteContent", noteRealm)
        return { noteRealm, undoTitle: "undo edit content" }
      },
    },
  ])(
    "navigates to note after confirming undo for $label",
    async ({ setup }) => {
      const { noteRealm, undoTitle } = setup()
      renderNoteUndoButton()

      await clickUndoButton(undoTitle)
      await clickDialogOk()

      expect(mockedPush).toHaveBeenCalledWith({
        name: "noteShow",
        params: {
          noteId: String(noteRealm.id),
        },
      })
    }
  )

  it("undo create note trashes the note and navigates to the notebook fallback", async () => {
    const noteRealm = makeMe.aNoteRealm.please()
    refreshNoteRealms(noteRealm)
    noteEditingHistory.createNote(noteRealm.id)
    const trashSpy = mockSdkService(NoteController, "trashNote", noteRealm)

    renderNoteUndoButton()
    await clickUndoButton("undo create note")
    await clickDialogOk()

    expect(trashSpy).toHaveBeenCalledWith({
      path: { note: noteRealm.id },
      body: { referenceHandling: "LEAVE_DEAD_LINKS" },
    })
    expect(mockedPush).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId: noteRealm.notebookRealm.notebook.id },
    })
  })

  it("does not navigate when confirmation is cancelled", async () => {
    const noteRealm = makeMe.aNoteRealm.title("Original title").please()
    noteEditingHistory.trashNote(noteRealm.id, "Original title", null)
    mockSdkService(NoteController, "undoTrashNote", noteRealm)
    renderNoteUndoButton()

    await clickUndoButton("undo trash note")
    await clickDialogCancel()

    expect(mockedPush).not.toHaveBeenCalled()
  })

  describe("discard", () => {
    it.each([
      {
        action: "edit title",
        setup: (
          noteRealm1: ReturnType<typeof makeMe.aNoteRealm.please>,
          noteRealm2: ReturnType<typeof makeMe.aNoteRealm.please>
        ) => {
          noteEditingHistory.addEditingToUndoHistory(
            noteRealm2.id,
            "edit title",
            "Old Title 2"
          )
          noteEditingHistory.addEditingToUndoHistory(
            noteRealm1.id,
            "edit title",
            "Old Title 1"
          )
        },
        undoTitle: "undo edit title",
      },
      {
        action: "edit content",
        setup: (
          noteRealm1: ReturnType<typeof makeMe.aNoteRealm.please>,
          noteRealm2: ReturnType<typeof makeMe.aNoteRealm.please>
        ) => {
          noteEditingHistory.addEditingToUndoHistory(
            noteRealm2.id,
            "edit content",
            "Old Content 2"
          )
          noteEditingHistory.addEditingToUndoHistory(
            noteRealm1.id,
            "edit content",
            "Old Content 1"
          )
        },
        undoTitle: "undo edit content",
      },
    ])(
      "discards $action item and shows next item",
      async ({ setup, undoTitle }) => {
        const { noteRealm1, noteRealm2 } = setupTwoCachedNotes()
        setup(noteRealm1, noteRealm2)
        renderNoteUndoButton()

        await clickUndoButton(undoTitle)

        expectConfirmUndoVisible()
        expectNoteTitleVisible("First Note")

        await clickDialogDiscard()

        expectConfirmUndoVisible()
        expectNoteTitleVisible("Second Note")
        expectNoteTitleHidden("First Note")
      }
    )

    it("closes dialog when discarding the last undo item", async () => {
      const note = makeMe.aNote.please()
      noteEditingHistory.addEditingToUndoHistory(note.id, "edit title", "Old")
      renderNoteUndoButton()

      await clickUndoButton("undo edit title")
      expectConfirmUndoVisible()

      await clickDialogDiscard()

      expectConfirmUndoHidden()
    })
  })
})
