import makeMe from "donut-test-fixtures/makeMe"
import { describe, it, expect, vi } from "vitest"
import {
  mountNoteUndoButton,
  noteEditingHistory,
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

describe("NoteUndoButton visibility", () => {
  it("does not show when there is nothing to undo", () => {
    const wrapper = mountNoteUndoButton()
    expect(wrapper.find("button").exists()).toBe(false)
  })

  it.each([
    {
      setup: (noteId: number) => noteEditingHistory.deleteNote(noteId),
      expectedTitle: "undo delete note",
    },
    {
      setup: (noteId: number) => noteEditingHistory.createNote(noteId),
      expectedTitle: "undo create note",
    },
    {
      setup: (noteId: number) =>
        noteEditingHistory.moveNote(noteId, { folderId: null, notebookId: 1 }),
      expectedTitle: "undo move note",
    },
  ])(
    "shows with title $expectedTitle when undo is available",
    ({ setup, expectedTitle }) => {
      const note = makeMe.aNote.please()
      setup(note.id)
      const wrapper = mountNoteUndoButton()
      expect(wrapper.find("button").attributes("title")).toBe(expectedTitle)
    }
  )
})
