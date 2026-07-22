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
import {
  createFolderPageRouter,
  folderNameConflictMessage,
  mountCrossNotebookFolderMovePage,
  mountCrossNotebookRootMovePage,
  mountFolderPage,
  openFolderSettingsTab,
  resolveTopConfirm,
  selectCrossNotebookDestination,
  selectDestinationNotebook,
  stubRouterPush,
  submitMoveForm,
} from "@tests/pages/folderPageTestSupport"
import type { Router } from "vue-router"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage move destinations", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  describe("move", () => {
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

      await openFolderSettingsTab(wrapper)
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
  })
})
