import { resetNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, it, expect } from "vitest"

describe("noteUndo", () => {
  describe("addEditingToUndoHistory", () => {
    it("accumulates continuous same-field edits to the same note", () => {
      const noteUndo = resetNoteStore().noteUndo
      const note1 = makeMe.aNote.please()
      noteUndo.addEditingToUndoHistory(note1.id, "edit title", "Original Title")
      noteUndo.addEditingToUndoHistory(note1.id, "edit title", "Original Title")

      expect(noteUndo.noteUndoHistories).toHaveLength(1)
      expect(noteUndo.noteUndoHistories[0]).toMatchObject({
        type: "edit title",
        textContent: "Original Title",
      })
    })

    it("creates a new entry when switching between title and content", () => {
      const noteUndo = resetNoteStore().noteUndo
      const note1 = makeMe.aNote.please()
      noteUndo.addEditingToUndoHistory(note1.id, "edit title", "Title")
      noteUndo.addEditingToUndoHistory(note1.id, "edit content", "Body")

      expect(noteUndo.noteUndoHistories).toHaveLength(2)
    })

    it("creates a new entry for title edit after trash note", () => {
      const noteUndo = resetNoteStore().noteUndo
      const note1 = makeMe.aNote.please()
      noteUndo.addEditingToUndoHistory(note1.id, "edit title", "Title")
      noteUndo.trashNote(note1.id, "Title", null)
      noteUndo.addEditingToUndoHistory(note1.id, "edit title", "New Title")

      expect(noteUndo.noteUndoHistories).toHaveLength(3)
    })
  })

  describe("createNote", () => {
    it("allows multiple create-note entries", () => {
      const noteUndo = resetNoteStore().noteUndo
      noteUndo.createNote(makeMe.aNote.please().id)
      noteUndo.createNote(makeMe.aNote.please().id)

      expect(noteUndo.noteUndoHistories).toHaveLength(2)
    })

    it("creates a new entry for title edit after create note", () => {
      const noteUndo = resetNoteStore().noteUndo
      const note1 = makeMe.aNote.please()
      noteUndo.createNote(note1.id)
      noteUndo.addEditingToUndoHistory(note1.id, "edit title", "New Title")

      expect(noteUndo.noteUndoHistories).toHaveLength(2)
    })
  })

  describe("moveNote", () => {
    it("adds a move-note entry with original location", () => {
      const noteUndo = resetNoteStore().noteUndo
      const note1 = makeMe.aNote.please()
      noteUndo.moveNote(note1.id, { folderId: null, notebookId: 42 })

      expect(noteUndo.noteUndoHistories[0]).toMatchObject({
        type: "move note",
        noteId: note1.id,
        originalFolderId: null,
        originalNotebookId: 42,
      })
    })
  })

  it("stays empty when discarding with no remaining history", () => {
    const noteUndo = resetNoteStore().noteUndo

    noteUndo.discardUndo()

    expect(noteUndo.noteUndoHistories).toHaveLength(0)
  })
})
