import {
  NoteController,
  RelationController,
  TextContentController,
} from "@generated/donut-backend-api/sdk.gen"
import type { Router } from "vue-router"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { resetNoteStore } from "@/store/noteStore"
import makeMe from "donut-test-fixtures/makeMe"
import { mockSdkService, wrapSdkError } from "@tests/helpers"
import { useNoteStore } from "@/store/noteStore"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import { describe, it, expect, vi, beforeEach } from "vitest"

describe("noteStore", () => {
  const note = makeMe.aNoteRealm.please()
  const noteStore = useNoteStore()
  const routerReplace = vi.fn()
  const routerPush = vi.fn()
  const router = {
    replace: routerReplace,
    push: routerPush,
  } as unknown as Router

  beforeEach(() => {
    vi.clearAllMocks()
    resetNoteStore()
  })

  describe("undo create note", () => {
    const parentNote = makeMe.aNoteRealm.please()

    beforeEach(() => {
      mockSdkService(NoteController, "trashNote", parentNote)
    })

    it("should remove the created note from cache after undo", async () => {
      resetNoteStore()
      const noteUndo = noteStore.noteUndo

      noteStore.refreshNoteRealm(note)
      noteUndo.createNote(note.id)

      expect(noteStore.refOfNoteRealm(note.id).value).toBeTruthy()

      await noteStore.undo(router)

      expect(noteStore.refOfNoteRealm(note.id).value).toBeUndefined()
    })
  })

  describe("completeContent", () => {
    let updateNoteContentSpy: ReturnType<typeof mockSdkService>
    let showNoteSpy: ReturnType<typeof mockSdkService>
    let noteRef

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteContentSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        note
      )
      showNoteSpy = mockSdkService(NoteController, "showNote", note)
      noteRef = noteStore.refOfNoteRealm(note.id)
    })

    it("does nothing when no completion value is provided", async () => {
      await noteStore.completeContent(note.id)
      expect(updateNoteContentSpy).not.toHaveBeenCalled()
    })

    it("updates note content with completion", async () => {
      noteRef.value = { ...note, note: { content: "Hello " } }

      await noteStore.completeContent(note.id, {
        content: "Hello world!",
      })

      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          content: "Hello world!",
        },
      })
    })

    it("loads note first if not in storage", async () => {
      noteRef.value = undefined

      await noteStore.completeContent(note.id, {
        content: "<p>Desc</p>world!",
      })

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
      expect(updateNoteContentSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          content: "<p>Desc</p>world!",
        },
      })
    })
  })

  describe("appendDictatedText", () => {
    it("loads the original body before appending when the note is absent", async () => {
      const original = makeMe.aNoteRealm.content("Original body.").please()
      const showNoteSpy = mockSdkService(NoteController, "showNote", original)
      const updateContentSpy = mockSdkService(
        TextContentController,
        "updateNoteContent",
        original
      )

      await noteStore.appendDictatedText(original.id, "New passage.")

      expect(showNoteSpy).toHaveBeenCalledWith({ path: { note: original.id } })
      expect(updateContentSpy).toHaveBeenCalledWith({
        path: { note: original.id },
        body: { content: "Original body. New passage." },
      })
    })
  })

  describe("move note", () => {
    it("refreshes sidebar structural listings after moving to a folder", async () => {
      mockSdkService(RelationController, "moveNoteToFolder", [note])
      noteStore.refreshNoteRealm(note)
      const before = sidebarStructuralRefreshKey.value
      await noteStore.moveNote(note.id, { folderId: 99 })
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
    })

    it("refreshes sidebar structural listings after moving to a notebook root", async () => {
      mockSdkService(RelationController, "moveNoteToNotebookRootInNotebook", [
        note,
      ])
      noteStore.refreshNoteRealm(note)
      const before = sidebarStructuralRefreshKey.value
      await noteStore.moveNote(note.id, {
        notebookId: note.notebookRealm.notebook.id,
      })
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
    })
  })

  describe("upload note image", () => {
    const file = new File(["png"], "Blue.PNG", { type: "image/png" })

    it("refreshes sidebar structural listings after an accepted upload", async () => {
      mockSdkService(NoteController, "uploadNoteImage", note)
      const before = sidebarStructuralRefreshKey.value
      await noteStore.uploadNoteImage(note.id, file)
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
    })

    it("leaves sidebar structural listings alone after a refused upload", async () => {
      vi.spyOn(NoteController, "uploadNoteImage").mockResolvedValue(
        wrapSdkError("refused")
      )
      const before = sidebarStructuralRefreshKey.value
      await expect(noteStore.uploadNoteImage(note.id, file)).rejects.toThrow(
        "refused"
      )
      expect(sidebarStructuralRefreshKey.value).toBe(before)
    })
  })

  describe("focusNoteRealm", () => {
    it("refreshes cache, sidebar listings, and navigates to the note", async () => {
      const before = sidebarStructuralRefreshKey.value

      await noteStore.focusNoteRealm(router, note)

      expect(noteStore.refOfNoteRealm(note.id).value).toBeTruthy()
      expect(sidebarStructuralRefreshKey.value).toBe(before + 1)
      expect(routerReplace).toHaveBeenCalledWith(noteShowLocation(note.id))
    })
  })
})
