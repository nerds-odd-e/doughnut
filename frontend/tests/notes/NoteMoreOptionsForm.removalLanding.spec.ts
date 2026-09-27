import type { FolderListing, NoteRealm } from "@generated/donut-backend-api"
import {
  NoteController,
  NotebookFolderController,
} from "@generated/donut-backend-api/sdk.gen"
import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
import { noteShowLocation } from "@/routes/noteShowLocation"
import { useNoteStore } from "@/store/noteStore"
import usePopups from "@/components/commons/Popups/usePopups"
import { flushPromises } from "@vue/test-utils"
import { sidebarStructuralRefreshKey } from "@/components/notes/sidebarStructuralRefresh"
import {
  renderer,
  setupNoteMoreOptionsTrashFormTests,
  loadingModalMask,
  noteMoreOptionsTrashFormRouter as router,
} from "./noteMoreOptionsTrashTestSupport"
import makeMe from "donut-test-fixtures/makeMe"
import {
  mockSdkService,
  mockSdkServiceWithImplementation,
  testFolderStub,
} from "@tests/helpers"
import { afterEach, describe, expect, it, vi } from "vitest"

setupNoteMoreOptionsTrashFormTests()

describe("where the person lands once a note is removed", () => {
  const notebookId = 71
  const peer = (title: string) =>
    makeMe.aNoteRealm
      .title(title)
      .inNotebook(notebookId)
      .inFolder(901, "Work")
      .please()
  const [a, b, c] = [peer("A"), peer("B"), peer("C")]

  type Removal = "trash" | "delete"
  const trash: Removal = "trash"
  const permanentlyDelete: Removal = "delete"

  const prepareRemoval = async (
    remove: Removal,
    realm: NoteRealm,
    listing: FolderListing
  ) => {
    const storage = useNoteStore()
    const displayedRealm =
      remove === "delete"
        ? {
            ...realm,
            ancestorFolders: [
              testFolderStub(900, "_trash"),
              ...(realm.ancestorFolders ?? []),
            ],
          }
        : realm
    storage.refreshNoteRealm(displayedRealm)
    const listingSpy = mockSdkService(
      NotebookFolderController,
      "listNotebookFolderListing",
      listing
    )
    const trashedRealm = {
      ...realm,
      ancestorFolders: [testFolderStub(900, "_trash")],
    }
    mockSdkService(NoteController, "trashNote", trashedRealm)
    mockSdkService(NoteController, "permanentlyDeleteNote", undefined)
    const wrapper = renderer.withProps({ note: realm.note }).mount()
    await flushPromises()
    await router.push(noteShowLocation(realm.id))
    const confirmRemoval = async () => {
      await wrapper
        .find(
          `button[title="${remove === "trash" ? "Trash note (d)" : "Permanently delete note (d)"}"]`
        )
        .trigger("click")
      await flushPromises()
      usePopups().popups.done(true)
      await flushPromises()
      await vi.waitFor(() => expect(loadingModalMask()).toBeNull())
    }
    return { storage, displayedRealm, trashedRealm, listingSpy, confirmRemoval }
  }

  const removeWithPeers = async (
    remove: Removal,
    realm: NoteRealm,
    listing: FolderListing
  ) => {
    const prepared = await prepareRemoval(remove, realm, listing)
    await prepared.confirmRemoval()
    return prepared.listingSpy
  }

  const notesListing = (...realms: NoteRealm[]): FolderListing => ({
    noteTopologies: realms.map((r) => r.note.noteTopology),
  })

  afterEach(() => localStorage.removeItem(PEER_SORT_STORAGE_KEY))

  describe.each([
    { removal: "trashing", remove: trash },
    { removal: "permanently deleting", remove: permanentlyDelete },
  ])("after $removal", ({ remove }) => {
    it("opens the next note before refreshing the sidebar", async () => {
      const {
        storage,
        displayedRealm,
        trashedRealm,
        listingSpy,
        confirmRemoval,
      } = await prepareRemoval(remove, b, notesListing(c, a, b))
      const keyBefore = sidebarStructuralRefreshKey.value
      const off = router.afterEach(() => {
        if (remove === "trash") {
          expect(storage.refOfNoteRealm(b.id).value).toEqual(displayedRealm)
        } else {
          expect(storage.isNotePermanentlyRemoved(b.id)).toBe(true)
        }
        expect(sidebarStructuralRefreshKey.value).toBe(keyBefore)
      })
      try {
        await confirmRemoval()
      } finally {
        off()
      }

      expect(listingSpy).toHaveBeenCalledWith({
        path: { notebook: notebookId },
        query: { parent: 901 },
      })
      expect(router.currentRoute.value.fullPath).toBe(
        router.resolve(noteShowLocation(c.id)).fullPath
      )
      expect(sidebarStructuralRefreshKey.value).toBe(keyBefore + 1)
      if (remove === "trash")
        expect(storage.refOfNoteRealm(b.id).value).toEqual(trashedRealm)
    })

    it("opens the previous note when removing the last one", async () => {
      await removeWithPeers(remove, c, notesListing(c, a, b))

      expect(router.currentRoute.value.fullPath).toBe(
        router.resolve(noteShowLocation(b.id)).fullPath
      )
    })

    it("follows the sidebar order chosen in this browser", async () => {
      localStorage.setItem(
        PEER_SORT_STORAGE_KEY,
        JSON.stringify({ field: "title", direction: "desc" })
      )

      await removeWithPeers(remove, b, notesListing(a, b, c))

      expect(router.currentRoute.value.fullPath).toBe(
        router.resolve(noteShowLocation(a.id)).fullPath
      )
    })

    it("opens the folder page when only subfolders and files remain beside the note", async () => {
      await removeWithPeers(remove, a, {
        ...notesListing(a),
        folders: [testFolderStub(902, "Sub")],
        attachments: [{ id: 5, filename: "B.png" }],
      })

      expect(router.currentRoute.value.fullPath).toBe(
        router.resolve({
          name: "folderPage",
          params: { notebookId: String(notebookId), folderId: "901" },
        }).fullPath
      )
    })
  })

  it("shows busy loading while the listing is read, before sending trash", async () => {
    useNoteStore().refreshNoteRealm(a)
    let resolveListing!: (listing: FolderListing) => void
    mockSdkServiceWithImplementation(
      NotebookFolderController,
      "listNotebookFolderListing",
      () =>
        new Promise((resolve) => {
          resolveListing = resolve
        })
    )
    const trashSpy = mockSdkService(NoteController, "trashNote", a)
    const wrapper = renderer.withProps({ note: a.note }).mount()
    await flushPromises()
    await wrapper.find('button[title="Trash note (d)"]').trigger("click")
    await flushPromises()
    usePopups().popups.done(true)
    await flushPromises()
    expect(loadingModalMask()).not.toBeNull()
    expect(trashSpy).not.toHaveBeenCalled()
    resolveListing(notesListing(a))
    await vi.waitFor(() => expect(loadingModalMask()).toBeNull())
    expect(trashSpy).toHaveBeenCalledOnce()
    wrapper.unmount()
  })

  it("opens the notebook page when trashing the only note at the notebook root", async () => {
    const realm = makeMe.aNoteRealm.inNotebook(notebookId).please()

    const listingSpy = await removeWithPeers(trash, realm, notesListing(realm))

    expect(listingSpy).toHaveBeenCalledWith({
      path: { notebook: notebookId },
      query: undefined,
    })
    expect(router.currentRoute.value.fullPath).toBe(
      router.resolve({
        name: "notebookPage",
        params: { notebookId },
      }).fullPath
    )
  })
})
