import NoteEditingHistory from "@/store/NoteEditingHistory"
import makeMe from "@tests/fixtures/makeMe"
import { describe, it, expect, beforeEach } from "vitest"

describe("storeUndoCommand", () => {
  const note = makeMe.aNoteRealm.title("Dummy Title").please()

  describe("addEditingToUndoHistory", () => {
    it("should push textContent into store state noteUndoHistories ", () => {
      const histories = new NoteEditingHistory()
      histories.addEditingToUndoHistory(
        note.id,
        "edit title",
        note.note.noteTopology.title
      )

      expect(histories.noteUndoHistories.length).toEqual(1)
    })

    it("should accumulate continuous title edits to the same note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(
        note1.id,
        "edit title",
        "Original Title"
      )
      expect(histories.noteUndoHistories.length).toEqual(1)

      // Second edit to same note's title should not create a new entry
      histories.addEditingToUndoHistory(
        note1.id,
        "edit title",
        "Original Title"
      )
      expect(histories.noteUndoHistories.length).toEqual(1)
      expect(histories.noteUndoHistories[0]!.textContent).toBe("Original Title")
    })

    it("should create new entry for title edit to different note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      const note2 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title 1")
      histories.addEditingToUndoHistory(note2.id, "edit title", "Title 2")

      expect(histories.noteUndoHistories.length).toEqual(2)
    })

    it("should accumulate continuous details edits to the same note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(
        note1.id,
        "edit details",
        "Original Details"
      )
      expect(histories.noteUndoHistories.length).toEqual(1)

      // Second edit to same note's details should not create a new entry
      histories.addEditingToUndoHistory(
        note1.id,
        "edit details",
        "Original Details"
      )
      expect(histories.noteUndoHistories.length).toEqual(1)
      expect(histories.noteUndoHistories[0]!.textContent).toBe(
        "Original Details"
      )
    })

    it("should create new entry for details edit to different note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      const note2 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit details", "Details 1")
      histories.addEditingToUndoHistory(note2.id, "edit details", "Details 2")

      expect(histories.noteUndoHistories.length).toEqual(2)
    })

    it("should create new entry when switching between title and details for same note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title")
      histories.addEditingToUndoHistory(note1.id, "edit details", "Details")

      expect(histories.noteUndoHistories.length).toEqual(2)
    })

    it("should create new entry for title edit after details edit to same note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit details", "Details")
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title")

      expect(histories.noteUndoHistories.length).toEqual(2)
    })

    it("should create new entry for title edit after delete note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.addEditingToUndoHistory(note1.id, "edit title", "Title")
      histories.deleteNote(note1.id)
      histories.addEditingToUndoHistory(note1.id, "edit title", "New Title")

      expect(histories.noteUndoHistories.length).toEqual(3)
    })
  })

  describe("createNote", () => {
    it("should add create note entry to undo history", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.createNote(note1.id)

      expect(histories.noteUndoHistories.length).toEqual(1)
      expect(histories.noteUndoHistories[0]!.type).toBe("create note")
      expect(histories.noteUndoHistories[0]!.noteId).toBe(note1.id)
    })

    it("should allow multiple create note entries", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      const note2 = makeMe.aNote.please()
      histories.createNote(note1.id)
      histories.createNote(note2.id)

      expect(histories.noteUndoHistories.length).toEqual(2)
    })

    it("should create new entry for title edit after create note", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      histories.createNote(note1.id)
      histories.addEditingToUndoHistory(note1.id, "edit title", "New Title")

      expect(histories.noteUndoHistories.length).toEqual(2)
    })
  })

  describe("moveNote", () => {
    it("should add move note entry to undo history", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      const parentNote = makeMe.aNote.please()
      const previousSibling = makeMe.aNote.please()
      histories.moveNote(note1.id, parentNote.id, previousSibling.id)

      expect(histories.noteUndoHistories.length).toEqual(1)
      expect(histories.noteUndoHistories[0]!.type).toBe("move note")
      expect(histories.noteUndoHistories[0]!.noteId).toBe(note1.id)
      expect(histories.noteUndoHistories[0]!.originalParentId).toBe(
        parentNote.id
      )
      expect(histories.noteUndoHistories[0]!.previousSiblingId).toBe(
        previousSibling.id
      )
    })

    it("should store null previousSiblingId when note was first child", () => {
      const histories = new NoteEditingHistory()
      const note1 = makeMe.aNote.please()
      const parentNote = makeMe.aNote.please()
      histories.moveNote(note1.id, parentNote.id, null)

      expect(histories.noteUndoHistories[0]!.previousSiblingId).toBeNull()
    })
  })

  describe("popUndoHistory", () => {
    let initialUndoCount
    const histories = new NoteEditingHistory()

    beforeEach(() => {
      histories.addEditingToUndoHistory(
        note.id,
        "edit details",
        note.note.details
      )
      initialUndoCount = histories.noteUndoHistories.length
    })

    it("should undo to last history", () => {
      histories.popUndoHistory()

      expect(histories.noteUndoHistories.length).toEqual(initialUndoCount - 1)
    })

    it("should not undo to last history if there is no more history", () => {
      histories.popUndoHistory()
      histories.popUndoHistory()
      histories.popUndoHistory()

      expect(histories.noteUndoHistories.length).toEqual(0)
    })
  })
})
