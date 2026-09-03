import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
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
    it("keeps soft-deleted conflicts inline, preserves typed conflicts on cancel, and merges on retry", async () => {
      const { wrapper, folderRealm } = mountFolderPage(router, 10, "Dup")
      const targetFolder = makeMe.aFolder
        .folder(99, folderRealm.folder.name)
        .please()

      const moveSpy = vi
        .spyOn(NotebookController, "moveFolder")
        .mockResolvedValueOnce(
          wrapSdkError({
            status: 409,
            errorType: "SOFT_DELETED_TITLE_CONFLICT",
            message: softDeletedTitleConflictMessage,
          })
        )
        .mockResolvedValue(
          wrapSdkError({
            message: folderNameConflictMessage,
            errorType: "FOLDER_NAME_CONFLICT",
          })
        )
      const pushSpy = stubRouterPush(router)

      await submitMoveForm(wrapper)
      expect(usePopups().popups.peek()).toHaveLength(0)
      expect(wrapper.text()).toContain(softDeletedTitleConflictMessage)

      await submitMoveForm(wrapper)
      expect(usePopups().popups.peek()?.[0]?.type).toBe("confirm")
      expect(usePopups().popups.peek()?.[0]?.message).toContain(
        "Merge into it?"
      )

      resolveTopConfirm(false)
      await flushPromises()
      expect(wrapper.text()).toContain(folderNameConflictMessage)
      expect(moveSpy).toHaveBeenCalledTimes(2)
      expect(pushSpy).not.toHaveBeenCalled()

      await submitMoveForm(wrapper)
      moveSpy.mockResolvedValueOnce(wrapSdkResponse(targetFolder) as never)
      resolveTopConfirm(true)
      await flushPromises()

      expect(moveSpy).toHaveBeenCalledTimes(4)
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
  })
})
