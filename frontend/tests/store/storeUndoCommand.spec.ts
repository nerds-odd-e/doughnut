import NoteEditingHistory from "@/store/NoteEditingHistory"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, it, expect, beforeEach } from "vitest"

describe("storeUndoCommand", () => {
  const note = makeMe.aNoteRealm.title("Dummy Title").please()

  describe("addEditingToUndoHistory", () => {
    it("pushes an edit into noteUndoHistories", () => {
      const histories = new NoteEditingHistory()
      histories.addEditingToUndoHistory(
        note.id,
        "edit title",
        note.note.noteTopology.title
      )

      expect(histories.noteUndoHistories).toHaveLength(1)
    })

    it("accumulates continuous same-field edits to the same note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(
        note1.id,
        "edit title",
        "Original Title"
      )
      histories.addEditingToUndoHistory(
        note1.id,
        "edit title",
        "Original Title"
      )

      expect(histories.noteUndoHistories).toHaveLength(1)
      expect(histories.noteUndoHistories[0]!.textContent).toBe("Original Title")
    })

    it("creates a new entry for the same field on a different note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      const note2 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title 1")
      histories.addEditingToUndoHistory(note2.id, "edit title", "Title 2")

      expect(histories.noteUndoHistories).toHaveLength(2)
    })

    it("creates a new entry when switching between title and content", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title")
      histories.addEditingToUndoHistory(note1.id, "edit content", "Body")

      expect(histories.noteUndoHistories).toHaveLength(2)
    })

    it("creates a new entry for title edit after delete note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title")
      histories.deleteNote(note1.id)
      histories.addEditingToUndoHistory(note1.id, "edit title", "New Title")

      expect(histories.noteUndoHistories).toHaveLength(3)
    })
  })

  describe("createNote", () => {
    it("adds a create-note entry", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.createNote(note1.id)

      expect(histories.noteUndoHistories[0]).toMatchObject({
        type: "create note",
        noteId: note1.id,
      })
    })

    it("allows multiple create-note entries", () => {
      const histories = new NoteEditingHistory()
      histories.createNote(makeMe.aNote.please().id)
      histories.createNote(makeMe.aNote.please().id)

      expect(histories.noteUndoHistories).toHaveLength(2)
    })

    it("creates a new entry for title edit after create note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.createNote(note1.id)
      histories.addEditingToUndoHistory(note1.id, "edit title", "New Title")

      expect(histories.noteUndoHistories).toHaveLength(2)
    })
  })

  describe("moveNote", () => {
    it("adds a move-note entry with original location", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.moveNote(note1.id, { folderId: null, notebookId: 42 })

      expect(histories.noteUndoHistories[0]).toMatchObject({
        type: "move note",
        noteId: note1.id,
        originalFolderId: null,
        originalNotebookId: 42,
      })
    })
  })

  describe("popUndoHistory", () => {
    let initialUndoCount: number
    const histories = new NoteEditingHistory()

    beforeEach(() => {
      histories.addEditingToUndoHistory(
        note.id,
        "edit content",
        note.note.content
      )
      initialUndoCount = histories.noteUndoHistories.length
    })

    it("pops the last history entry", () => {
      histories.popUndoHistory()

      expect(histories.noteUndoHistories).toHaveLength(initialUndoCount - 1)
    })

    it("stays empty when popping with no remaining history", () => {
      histories.popUndoHistory()
      histories.popUndoHistory()
      histories.popUndoHistory()

      expect(histories.noteUndoHistories).toHaveLength(0)
    })
  })
})
