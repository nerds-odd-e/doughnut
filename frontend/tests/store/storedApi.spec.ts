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
      updateNoteDetailsSpy = mockSdkService("updateNoteDetails", note)
      showNoteSpy = mockSdkService("showNote", note)
      noteRef = storageAccessor.value.refOfNoteRealm(note.id)
    })

    it("should do nothing when no completion value is provided", async () => {
      const sa = storageAccessor.value.storedApi()
      await sa.completeDetails(note.id)
      expect(updateNoteDetailsSpy).not.toHaveBeenCalled()
    })

    it("should update note details with completion patch", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello " } }

      const patch = "--- a\n+++ b\n@@ -1,1 +1,2 @@\n Hello \n+world!"
      await sa.completeDetails(note.id, {
        patch,
      })

      expect(updateNoteDetailsSpy).toHaveBeenCalled()
      const call = updateNoteDetailsSpy.mock.calls[0]![0] as {
        body: { details: string }
      }
      expect(call.body.details).toContain("Hello")
      expect(call.body.details).toContain("world!")
    })

    it("should apply patch that deletes characters before adding completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello world" } }

      const patch = "--- a\n+++ b\n@@ -1,1 +1,1 @@\n-Hello world\n+Hello !\n"
      await sa.completeDetails(note.id, {
        patch,
      })

      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: expect.stringContaining("Hello !"),
        },
      })
    })

    it("should load note first if not in storage", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = undefined

      const patch = "--- a\n+++ b\n@@ -1,1 +1,2 @@\n <p>Desc</p>\n+world!\n"
      await sa.completeDetails(note.id, {
        patch,
      })

      expect(showNoteSpy).toHaveBeenCalledWith({
        path: { note: note.id },
      })
      expect(updateNoteDetailsSpy).toHaveBeenCalledWith({
        path: { note: note.id },
        body: {
          details: expect.stringContaining("world!"),
        },
      })
    })
  })
})
