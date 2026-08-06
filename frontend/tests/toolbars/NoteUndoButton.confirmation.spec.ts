import makeMe from "doughnut-test-fixtures/makeMe"
import { describe, it, expect, vi } from "vitest"
import { screen } from "@testing-library/vue"
import {
  clickUndoButton,
  expectConfirmUndoVisible,
  expectNoteTitleLink,
  noteEditingHistory,
  refreshNoteRealms,
  renderNoteUndoButton,
  setupNoteUndoButtonTests,
} from "./noteUndoButtonTestSupport"

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  const { noteUndoButtonRouterMockExports } = await import(
    "./noteUndoButtonMocks"
  )
  return noteUndoButtonRouterMockExports(actual)
})

setupNoteUndoButtonTests()

describe("NoteUndoButton confirmation dialog", () => {
  describe("when note is in cache", () => {
    it.each([
      {
        action: "delete note",
        setup: (noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>) =>
          noteEditingHistory.deleteNote(noteRealm.id),
        undoTitle: "undo delete note",
        message: /Are you sure you want to undo deleting /,
      },
      {
        action: "create note",
        setup: (noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>) =>
          noteEditingHistory.createNote(noteRealm.id),
        undoTitle: "undo create note",
        message: /Are you sure you want to undo creating /,
      },
      {
        action: "move note",
        setup: (noteRealm: ReturnType<typeof makeMe.aNoteRealm.please>) => {
          noteEditingHistory.moveNote(noteRealm.id, {
            folderId: null,
            notebookId: noteRealm.notebookRealm.notebook.id,
          })
        },
        undoTitle: "undo move note",
        message: /Are you sure you want to undo moving /,
      },
    ])(
      "shows confirmation with note title for $action",
      async ({ setup, undoTitle, message }) => {
        const noteRealm = makeMe.aNoteRealm.title("My Note").please()
        refreshNoteRealms(noteRealm)
        setup(noteRealm)
        renderNoteUndoButton()

        await clickUndoButton(undoTitle)

        expectConfirmUndoVisible()
        expect(screen.getByText(message)).toBeInTheDocument()
        expectNoteTitleLink("My Note")
      }
    )

    it("shows title and diff for edit title", async () => {
      const noteRealm = makeMe.aNoteRealm.title("Test Note").please()
      refreshNoteRealms(noteRealm)
      noteEditingHistory.addEditingToUndoHistory(
        noteRealm.id,
        "edit title",
        "Old Title"
      )
      renderNoteUndoButton()

      await clickUndoButton("undo edit title")

      expect(
        screen.getByText(/Are you sure you want to undo editing the title of /)
      ).toBeInTheDocument()
      expectNoteTitleLink("Test Note")
      expect(screen.getByText("Current")).toBeInTheDocument()
      expect(screen.getByText("Will restore to")).toBeInTheDocument()
    })

    it("shows title and diff for edit content", async () => {
      const noteRealm = makeMe.aNoteRealm.title("Content Note").please()
      refreshNoteRealms(noteRealm)
      noteEditingHistory.addEditingToUndoHistory(
        noteRealm.id,
        "edit content",
        "Old Content"
      )
      renderNoteUndoButton()

      await clickUndoButton("undo edit content")

      expect(
        screen.getByText(
          /Are you sure you want to undo editing the content of /
        )
      ).toBeInTheDocument()
      expectNoteTitleLink("Content Note")
      expect(screen.getByText("Current")).toBeInTheDocument()
      expect(screen.getByText("Will restore to")).toBeInTheDocument()
    })

    it("shows HTML tags as part of markdown content in diff view", async () => {
      const noteRealm = makeMe.aNoteRealm.title("Content Note").please()
      refreshNoteRealms(noteRealm)
      noteEditingHistory.addEditingToUndoHistory(
        noteRealm.id,
        "edit content",
        "<p>Old <strong>Content</strong> with <em>HTML</em></p>"
      )
      renderNoteUndoButton()

      await clickUndoButton("undo edit content")

      const diffContent = screen.getByText("Will restore to").parentElement
      expect(diffContent?.textContent).toContain(
        "<p>Old <strong>Content</strong> with <em>HTML</em></p>"
      )
    })
  })

  describe("when note is not in cache", () => {
    it.each([
      {
        action: "delete note",
        setup: (noteId: number) => noteEditingHistory.deleteNote(noteId),
        undoTitle: "undo delete note",
        message: (noteId: number) =>
          `Are you sure you want to undo deleting note id: ${noteId}\\?`,
      },
      {
        action: "edit title",
        setup: (noteId: number) =>
          noteEditingHistory.addEditingToUndoHistory(
            noteId,
            "edit title",
            "Old Title"
          ),
        undoTitle: "undo edit title",
        message: (noteId: number) =>
          `Are you sure you want to undo editing the title of note id: ${noteId}\\?`,
      },
      {
        action: "edit content",
        setup: (noteId: number) =>
          noteEditingHistory.addEditingToUndoHistory(
            noteId,
            "edit content",
            "Old Content"
          ),
        undoTitle: "undo edit content",
        message: (noteId: number) =>
          `Are you sure you want to undo editing the content of note id: ${noteId}\\?`,
      },
    ])(
      "shows confirmation with note id for $action when note is not cached",
      async ({ setup, undoTitle, message }) => {
        const note = makeMe.aNote.please()
        setup(note.id)
        renderNoteUndoButton()

        await clickUndoButton(undoTitle)

        expectConfirmUndoVisible()
        expect(
          screen.getByText(new RegExp(message(note.id)))
        ).toBeInTheDocument()
      }
    )
  })
})
