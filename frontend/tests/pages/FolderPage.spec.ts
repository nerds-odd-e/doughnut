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
  mountFolderPage,
  resolveTopConfirm,
  stubFolderPageListingMocks,
  submitMoveForm,
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
    stubFolderPageListingMocks()
  })

  describe("move", () => {
    it.each([
      {
        case: "RESOURCE_CONFLICT",
        error: {
          message: "A folder with this name already exists here.",
          errorType: "RESOURCE_CONFLICT",
        },
      },
      {
        case: "409 status",
        error: {
          status: 409,
          message: "A folder with this name already exists here.",
        },
      },
    ] as const)("shows merge confirm when move returns $case and retries with merge flag", async ({
      error,
    }) => {
      const { wrapper, folderRealm } = mountFolderPage(router, 10, "Dup")

      const moveSpy = vi
        .spyOn(NotebookController, "moveFolder")
        .mockResolvedValue(wrapSdkError(error))

      await submitMoveForm(wrapper)

      const popup = usePopups().popups.peek()?.[0]
      expect(popup?.type).toBe("confirm")
      expect(popup?.message).toContain("Merge into it?")

      moveSpy.mockResolvedValueOnce(
        wrapSdkResponse(folderRealm.folder) as never
      )
      resolveTopConfirm(true)
      await flushPromises()

      expect(moveSpy).toHaveBeenCalledTimes(2)
      expect(moveSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({
          body: expect.objectContaining({ merge: true }),
        })
      )

      wrapper.unmount()
    })

    it("shows error message when move 409 and user cancels merge", async () => {
      const { wrapper } = mountFolderPage(router, 10, "Dup")

      vi.spyOn(NotebookController, "moveFolder").mockResolvedValue(
        wrapSdkError({
          status: 409,
          message: "A folder with this name already exists here.",
        })
      )

      await submitMoveForm(wrapper)
      resolveTopConfirm(false)
      await flushPromises()

      expect(wrapper.text()).toContain(
        "A folder with this name already exists here."
      )
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

      const mounted = mountFolderPage(router, 2, "Beta", fetchFolderPage)
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

    it("navigates to the destination folder after a confirmed merge move", async () => {
      const { wrapper, folderRealm } = mountFolderPage(router, 10, "Shared")
      const targetFolder = makeMe.aFolder.folder(99, "Shared").please()

      vi.spyOn(NotebookController, "moveFolder")
        .mockResolvedValueOnce(
          wrapSdkError({
            message: "A folder with this name already exists here.",
            errorType: "RESOURCE_CONFLICT",
          })
        )
        .mockResolvedValueOnce(wrapSdkResponse(targetFolder) as never)

      const pushSpy = vi
        .spyOn(router, "push")
        .mockResolvedValue(undefined as never)

      await submitMoveForm(wrapper)
      resolveTopConfirm(true)
      await flushPromises()

      expect(pushSpy).toHaveBeenCalledWith({
        name: "folderPage",
        params: {
          notebookId: String(folderRealm.notebookRealm.notebook.id),
          folderId: String(targetFolder.id),
        },
      })

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

      const conflictMessage =
        "A note with this title already exists here but was deleted. Restore the deleted note (Undo delete), or choose another title."
      vi.spyOn(NotebookController, "dissolveFolder").mockResolvedValue(
        wrapSdkError({
          status: 409,
          errorType: "SOFT_DELETED_TITLE_CONFLICT",
          message: conflictMessage,
        })
      )

      await dissolveWithInitialConfirm(wrapper)

      expect(wrapper.text()).toContain(conflictMessage)

      wrapper.unmount()
    })
  })
})
