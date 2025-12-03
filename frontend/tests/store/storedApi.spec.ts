import type { Router } from "vue-router"
import createNoteStorage from "@/store/createNoteStorage"
import makeMe from "@tests/fixtures/makeMe"
import { mockSdkService } from "@tests/helpers"
import { useStorageAccessor } from "@/composables/useStorageAccessor"

describe("storedApiCollection", () => {
  const note = makeMe.aNoteRealm.please()
  const storageAccessor = useStorageAccessor()
  const routerReplace = vitest.fn()
  const router = { replace: routerReplace } as unknown as Router

  beforeEach(() => {
    storageAccessor.value = createNoteStorage()
  })

  describe("delete note", () => {
    let deleteNoteSpy: ReturnType<typeof mockSdkService<"deleteNote">>

    beforeEach(() => {
      deleteNoteSpy = mockSdkService("deleteNote", [note])
    })

    it("should call the api", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.deleteNote(router, note.id)
      expect(deleteNoteSpy).toHaveBeenCalledTimes(1)
      expect(deleteNoteSpy).toHaveBeenCalledWith({ path: { note: note.id } })
      expect(routerReplace).toHaveBeenCalledTimes(1)
    })
  })

  describe("completeDetails", () => {
    let updateNoteDetailsSpy: ReturnType<
      typeof mockSdkService<"updateNoteDetails">
    >
    let showNoteSpy: ReturnType<typeof mockSdkService<"showNote">>
    let noteRef

    beforeEach(() => {
      vi.clearAllMocks()
      updateNoteDetailsSpy = mockSdkService("updateNoteDetails", note)
      showNoteSpy = mockSdkService("showNote", note)
      noteRef = storageAccessor.value.refOfNoteRealm(note.id)
    })

    it("should do nothing when no completion value is provided", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.completeDetails(note.id)
      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()
    })

    it("should update note details with completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello " } }

      const patch = "--- a\n+++ b\n@@ -1,1 +1,1 @@\n-Hello \n+Hello world!\n"
      await sa.completeDetails(note.id, {
        patch,
      })

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "Hello world!",
        },
      })
    })

    it("should delete characters before adding completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello world" } }

      const patch = "--- a\n+++ b\n@@ -1,1 +1,1 @@\n-Hello world\n+Hello !\n"
      await sa.completeDetails(note.id, {
        patch,
      })

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "Hello !",
        },
      })
    })

    it("should load note first if not in storage", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = undefined

      const patch =
        "--- a\n+++ b\n@@ -1,1 +1,1 @@\n-<p>Desc</p>\n+<p>Desc</p>world!\n"
      await sa.completeDetails(note.id, {
        patch,
      })

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: "<p>Desc</p>world!",
        },
      })
    })

    it("should throw error for invalid patch format that doesn't start with '--- ' or contain '@@'", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "It is a" } }

      const invalidPatch = "-It is a\n+It is a bustling metropolis"
      await expect(
        sa.completeDetails(note.id, {
          patch: invalidPatch,
        })
      ).rejects.toThrow(
        "Invalid patch format: patch must be in unified diff format"
      )

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()
    })

    it("should throw error when patch doesn't change the content", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "It is a" } }

      // This patch has valid format but replaces content with itself (no change)
      const noChangePatch =
        "--- a\n+++ b\n@@ -1,1 +1,1 @@\n-It is a\n+It is a\n"
      await expect(
        sa.completeDetails(note.id, {
          patch: noChangePatch,
        })
      ).rejects.toThrow(
        "Patch did not modify the content: patch format may be invalid or patch has no effect"
      )

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()
    })

    it("should throw error when patch format is completely invalid", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "It is a" } }

      const invalidPatch = "just some random text"
      await expect(
        sa.completeDetails(note.id, {
          patch: invalidPatch,
        })
      ).rejects.toThrow(
        "Invalid patch format: patch must be in unified diff format"
      )

      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()
    })
  })
})
