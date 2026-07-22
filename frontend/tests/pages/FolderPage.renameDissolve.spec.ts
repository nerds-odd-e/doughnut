import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  dissolveWithInitialConfirm,
  folderNameConflictMessage,
  mountFolderPage,
  mountFolderPageReady,
  resolveTopConfirm,
  setRenameName,
  softDeletedTitleConflictMessage,
  submitRenameForm,
} from "@tests/pages/folderPageTestSupport"
import type { Router } from "vue-router"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage rename and dissolve", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
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
