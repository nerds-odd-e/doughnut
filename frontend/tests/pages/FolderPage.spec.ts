import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import {
  mockSdkService,
  testFolderStub,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  dissolveWithInitialConfirm,
  folderNameConflictMessage,
  mountCrossNotebookFolderMovePage,
  mountCrossNotebookRootMovePage,
  mountFolderPage,
  mountFolderPageReady,
  resolveTopConfirm,
  selectCrossNotebookDestination,
  selectDestinationNotebook,
  setRenameName,
  softDeletedTitleConflictMessage,
  stubRouterPush,
  submitMoveForm,
  submitRenameForm,
} from "@tests/pages/folderPageTestSupport"
import type { Router } from "vue-router"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  describe("move", () => {
    it.each([
      {
        case: "409 with FOLDER_NAME_CONFLICT",
        error: {
          status: 409,
          message: folderNameConflictMessage,
          errorType: "FOLDER_NAME_CONFLICT",
        },
      },
      {
        case: "typed signal without status",
        error: {
          message: folderNameConflictMessage,
          errorType: "FOLDER_NAME_CONFLICT",
        },
      },
    ] as const)("shows merge confirm when move returns $case, retries with merge, and navigates", async ({
      error,
    }) => {
      const { wrapper, folderRealm } = mountFolderPage(router, 10, "Dup")
      const targetFolder = makeMe.aFolder
        .folder(99, folderRealm.folder.name)
        .please()

      const moveSpy = vi
        .spyOn(NotebookController, "moveFolder")
        .mockResolvedValue(wrapSdkError(error))

      const pushSpy = stubRouterPush(router)

      await submitMoveForm(wrapper)

      const popup = usePopups().popups.peek()?.[0]
      expect(popup?.type).toBe("confirm")
      expect(popup?.message).toContain("Merge into it?")

      moveSpy.mockResolvedValueOnce(wrapSdkResponse(targetFolder) as never)
      resolveTopConfirm(true)
      await flushPromises()

      expect(moveSpy).toHaveBeenCalledTimes(2)
      expect(moveSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({
          body: expect.objectContaining({ merge: true }),
        })
      )
      expect(pushSpy).toHaveBeenCalledWith({
        name: "folderPage",
        params: {
          notebookId: String(folderRealm.notebookRealm.notebook.id),
          folderId: String(targetFolder.id),
        },
      })

      wrapper.unmount()
    })

    it("shows error message when move 409 and user cancels merge", async () => {
      const { wrapper } = mountFolderPage(router, 10, "Dup")

      vi.spyOn(NotebookController, "moveFolder").mockResolvedValue(
        wrapSdkError({
          status: 409,
          message: folderNameConflictMessage,
          errorType: "FOLDER_NAME_CONFLICT",
        })
      )

      await submitMoveForm(wrapper)
      resolveTopConfirm(false)
      await flushPromises()

      expect(wrapper.text()).toContain(folderNameConflictMessage)
      wrapper.unmount()
    })

    it("re-enables organize controls after moving into a neighbour folder", async () => {
      const alpha = testFolderStub(1, "Alpha")
      const beta = testFolderStub(2, "Beta")
      const realmAtRoot = makeMe.aFolderRealm.folder(2, "Beta").please()

      mockSdkService(NotebookController, "listNotebookFolderListing", {
        folders: [alpha],
      })

      const realmUnderAlpha: typeof realmAtRoot = {
        ...realmAtRoot,
        ancestorFolders: [alpha, beta],
      }
      let wrapper: ReturnType<typeof mountFolderPage>["wrapper"]
      const fetchFolderPage = vi.fn(async () => {
        await wrapper.setProps({ folderRealm: realmUnderAlpha })
      })

      const mounted = mountFolderPage(router, 2, "Beta", { fetchFolderPage })
      wrapper = mounted.wrapper
      await flushPromises()

      await wrapper
        .get('[data-testid="folder-move-parent-select"]')
        .setValue("1")
      await flushPromises()

      vi.spyOn(NotebookController, "moveFolder").mockResolvedValue(
        wrapSdkResponse(beta) as never
      )

      await submitMoveForm(wrapper)

      expect(fetchFolderPage).toHaveBeenCalled()
      expect(
        wrapper
          .find('[data-testid="folder-move-submit"]')
          .attributes("disabled")
      ).toBeUndefined()
      expect(
        wrapper
          .find('[data-testid="folder-dissolve-button"]')
          .attributes("disabled")
      ).toBeUndefined()

      wrapper.unmount()
    })

    it("sends destinationNotebookId and navigates after cross-notebook root move", async () => {
      const { wrapper, folderRealm, destinationNotebook } =
        await mountCrossNotebookRootMovePage(router, 10, "Moved")

      const moveSpy = vi
        .spyOn(NotebookController, "moveFolder")
        .mockResolvedValue(wrapSdkResponse(folderRealm.folder) as never)
      const pushSpy = stubRouterPush(router)

      await selectDestinationNotebook(wrapper, destinationNotebook.id)
      await submitMoveForm(wrapper)

      expect(moveSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: {
            notebook: folderRealm.notebookRealm.notebook.id,
            folder: folderRealm.folder.id,
          },
          body: {
            destinationNotebookId: destinationNotebook.id,
            merge: false,
          },
        })
      )
      expect(pushSpy).toHaveBeenCalledWith({
        name: "folderPage",
        params: {
          notebookId: String(destinationNotebook.id),
          folderId: String(folderRealm.folder.id),
        },
      })

      wrapper.unmount()
    })

    it("sends destinationNotebookId and newParentFolderId for cross-notebook folder move", async () => {
      const { wrapper, folderRealm, destinationNotebook, destParent } =
        await mountCrossNotebookFolderMovePage(router, 10, "Moved")

      const moveSpy = vi
        .spyOn(NotebookController, "moveFolder")
        .mockResolvedValue(wrapSdkResponse(folderRealm.folder) as never)

      await selectCrossNotebookDestination(
        wrapper,
        destinationNotebook.id,
        destParent.id
      )
      await submitMoveForm(wrapper)

      expect(moveSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          path: {
            notebook: folderRealm.notebookRealm.notebook.id,
            folder: folderRealm.folder.id,
          },
          body: {
            destinationNotebookId: destinationNotebook.id,
            newParentFolderId: destParent.id,
            merge: false,
          },
        })
      )

      wrapper.unmount()
    })

    it("retries cross-notebook folder move with merge after 409 conflict", async () => {
      const { wrapper, destinationNotebook, destParent } =
        await mountCrossNotebookFolderMovePage(router, 10, "Dup")
      const targetFolder = makeMe.aFolder.folder(99, "Dup").please()

      const moveSpy = vi
        .spyOn(NotebookController, "moveFolder")
        .mockResolvedValueOnce(
          wrapSdkError({
            status: 409,
            message: folderNameConflictMessage,
            errorType: "FOLDER_NAME_CONFLICT",
          })
        )
        .mockResolvedValueOnce(wrapSdkResponse(targetFolder) as never)
      const pushSpy = stubRouterPush(router)

      await selectCrossNotebookDestination(
        wrapper,
        destinationNotebook.id,
        destParent.id
      )
      await submitMoveForm(wrapper)
      resolveTopConfirm(true)
      await flushPromises()

      expect(moveSpy).toHaveBeenCalledTimes(2)
      expect(moveSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({
          body: {
            destinationNotebookId: destinationNotebook.id,
            newParentFolderId: destParent.id,
            merge: true,
          },
        })
      )
      expect(pushSpy).toHaveBeenCalledWith({
        name: "folderPage",
        params: {
          notebookId: String(destinationNotebook.id),
          folderId: String(targetFolder.id),
        },
      })

      wrapper.unmount()
    })

    it("shows inline error without merge prompt when move returns soft-deleted title conflict", async () => {
      const { wrapper } = mountFolderPage(router, 10, "Dup")

      vi.spyOn(NotebookController, "moveFolder").mockResolvedValue(
        wrapSdkError({
          status: 409,
          errorType: "SOFT_DELETED_TITLE_CONFLICT",
          message: softDeletedTitleConflictMessage,
        })
      )

      await submitMoveForm(wrapper)

      expect(usePopups().popups.peek()).toHaveLength(0)
      expect(wrapper.text()).toContain(softDeletedTitleConflictMessage)

      wrapper.unmount()
    })
  })

  describe("rename", () => {
    it("shows inline conflict error when rename returns 409 FOLDER_NAME_CONFLICT", async () => {
      const { wrapper } = await mountFolderPageReady(router, 10, "Original")

      const renameSpy = vi
        .spyOn(NotebookController, "renameFolder")
        .mockResolvedValue(
          wrapSdkError({
            status: 409,
            message: folderNameConflictMessage,
            errorType: "FOLDER_NAME_CONFLICT",
          })
        )

      await setRenameName(wrapper, "Existing")
      await submitRenameForm(wrapper)

      expect(renameSpy).toHaveBeenCalled()
      expect(wrapper.text()).toContain(folderNameConflictMessage)
      expect(usePopups().popups.peek()).toHaveLength(0)

      wrapper.unmount()
    })
  })

  describe("dissolve", () => {
    it("shows merge confirm when dissolve returns 409 and retries with merge=true", async () => {
      const { wrapper } = mountFolderPage(router, 20, "Mid")

      const dissolveSpy = vi
        .spyOn(NotebookController, "dissolveFolder")
        .mockResolvedValue(
          wrapSdkError({
            status: 409,
            message:
              "A folder with this name already exists at the destination: Inner",
            errorType: "FOLDER_NAME_CONFLICT",
          })
        )

      await dissolveWithInitialConfirm(wrapper)

      const mergePopup = usePopups().popups.peek()?.[0]
      expect(mergePopup?.type).toBe("confirm")
      expect(mergePopup?.message).toContain("Merge them?")

      dissolveSpy.mockResolvedValueOnce(wrapSdkResponse(undefined) as never)
      resolveTopConfirm(true)
      await flushPromises()

      expect(dissolveSpy).toHaveBeenCalledTimes(2)
      expect(dissolveSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({ query: { merge: true } })
      )

      wrapper.unmount()
    })

    it("shows inline error when dissolve returns soft-deleted title conflict", async () => {
      const { wrapper } = mountFolderPage(router, 20, "Mid")

      vi.spyOn(NotebookController, "dissolveFolder").mockResolvedValue(
        wrapSdkError({
          status: 409,
          errorType: "SOFT_DELETED_TITLE_CONFLICT",
          message: softDeletedTitleConflictMessage,
        })
      )

      await dissolveWithInitialConfirm(wrapper)

      expect(wrapper.text()).toContain(softDeletedTitleConflictMessage)

      wrapper.unmount()
    })
  })
})
