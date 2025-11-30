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

    it("should update note details with completion", async () => {
      const sa = storageAccessor.value.storedApi()
      noteRef.value = { ...note, note: { details: "Hello " } }

      await sa.completeDetails(note.id, {
        completion: "world!",
        deleteFromEnd: 0,
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

      await sa.completeDetails(note.id, {
        completion: "!",
        deleteFromEnd: 5,
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

      await sa.completeDetails(note.id, {
        completion: "world!",
        deleteFromEnd: 0,
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
  })
})
