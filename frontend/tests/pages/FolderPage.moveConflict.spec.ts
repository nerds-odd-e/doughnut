import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  folderNameConflictMessage,
  mountFolderPage,
  resolveTopConfirm,
  softDeletedTitleConflictMessage,
  stubRouterPush,
  submitMoveForm,
} from "@tests/pages/folderPageTestSupport"
import type { Router } from "vue-router"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage move conflicts", () => {
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
    ] as const)(
      "shows merge confirm when move returns $case, retries with merge, and navigates",
      async ({ error }) => {
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
      }
    )

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
})
