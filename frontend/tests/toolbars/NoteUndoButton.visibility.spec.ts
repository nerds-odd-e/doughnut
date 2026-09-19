import { NoteController } from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { describe, it, expect, vi } from "vitest"
import {
  mountNoteUndoButton,
  noteEditingHistory,
  setupTwoCachedNotes,
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
      setup: (noteId: number) =>
        noteEditingHistory.trashNote(noteId, "Note", null),
      expectedTitle: "undo trash note",
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

  describe("after a note is trashed and then permanently deleted", () => {
    const router = { replace: vi.fn() } as unknown as Router

    it("offers the other note's older entry instead of naming the deleted note", async () => {
      const { noteRealm1, noteRealm2 } = setupTwoCachedNotes()
      noteEditingHistory.addEditingToUndoHistory(
        noteRealm2.id,
        "edit content",
        "Old content 2"
      )
      noteEditingHistory.addEditingToUndoHistory(
        noteRealm1.id,
        "edit content",
        "Old content 1"
      )
      mockSdkService(NoteController, "trashNote", noteRealm1)
      mockSdkService(NoteController, "permanentlyDeleteNote", undefined)
      const storedApi = useStorageAccessor().value.storedApi()

      await storedApi.trashNote(router, noteRealm1.id, {
        referenceHandling: "LEAVE_DEAD_LINKS",
      })
      await storedApi.permanentlyDeleteNote(router, noteRealm1.id)

      const wrapper = mountNoteUndoButton()
      expect(wrapper.find("button").attributes("title")).toBe(
        "undo edit content"
      )
    })

    it("is absent when the only undo entry belonged to the deleted note", async () => {
      const noteRealm = makeMe.aNoteRealm.please()
      useStorageAccessor().value.refreshNoteRealm(noteRealm)
      noteEditingHistory.addEditingToUndoHistory(
        noteRealm.id,
        "edit content",
        "Old content"
      )
      mockSdkService(NoteController, "trashNote", noteRealm)
      mockSdkService(NoteController, "permanentlyDeleteNote", undefined)
      const storedApi = useStorageAccessor().value.storedApi()

      await storedApi.trashNote(router, noteRealm.id, {
        referenceHandling: "LEAVE_DEAD_LINKS",
      })
      await storedApi.permanentlyDeleteNote(router, noteRealm.id)

      const wrapper = mountNoteUndoButton()
      expect(wrapper.find("button").exists()).toBe(false)
    })
  })
})
