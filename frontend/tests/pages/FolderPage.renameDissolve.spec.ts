import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  dissolveWithInitialConfirm,
  editFolderPageName,
  folderPageNameEditor,
  folderNameConflictMessage,
  mountFolderPage,
  mountFolderPageReady,
  openFolderSettingsTab,
  resolveTopConfirm,
  softDeletedTitleConflictMessage,
} from "@tests/pages/folderPageTestSupport"
import type { Router } from "vue-router"

afterEach(() => {
  document.body.innerHTML = ""
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe("FolderPage rename and dissolve", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  describe("rename", () => {
    async function replacePendingFolderNameWith(finalName: string) {
      vi.useFakeTimers()
      const { wrapper } = await mountFolderPageReady(router, 10, "Original")
      const renameSpy = vi
        .spyOn(NotebookController, "renameFolder")
        .mockResolvedValue(wrapSdkResponse(undefined) as never)

      await editFolderPageName(wrapper, "Intermediate", false)
      await editFolderPageName(wrapper, finalName, false)
      await vi.runAllTimersAsync()
      await flushPromises()

      return { wrapper, renameSpy }
    }

    it("renders the folder name as an editable page heading", async () => {
      const { wrapper } = await mountFolderPageReady(router, 10, "Original")

      expect(folderPageNameEditor(wrapper).attributes("contenteditable")).toBe(
        "true"
      )

      wrapper.unmount()
    })

    it("saves a trimmed changed heading on blur and refreshes the page", async () => {
      const fetchFolderPage = vi.fn().mockResolvedValue(undefined)
      const { wrapper, folderRealm } = await mountFolderPageReady(
        router,
        10,
        "Original",
        { fetchFolderPage }
      )
      const renameSpy = vi
        .spyOn(NotebookController, "renameFolder")
        .mockResolvedValue(wrapSdkResponse(folderRealm.folder))

      await editFolderPageName(wrapper, "  Renamed  ")

      expect(renameSpy).toHaveBeenCalledWith({
        path: {
          notebook: folderRealm.notebookRealm.notebook.id,
          folder: folderRealm.folder.id,
        },
        body: { name: "Renamed" },
      })
      expect(fetchFolderPage).toHaveBeenCalledOnce()

      wrapper.unmount()
    })

    it("cancels pending rename when restored to saved name or blank", async () => {
      {
        const { wrapper, renameSpy } =
          await replacePendingFolderNameWith("Original")
        expect(renameSpy).not.toHaveBeenCalled()
        wrapper.unmount()
      }

      {
        const { wrapper, renameSpy } = await replacePendingFolderNameWith("   ")
        expect(renameSpy).not.toHaveBeenCalled()
        expect(wrapper.text()).toContain(
          "Folder name cannot be empty. Enter a name to rename this folder."
        )
        wrapper.unmount()
      }
    })

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

      await editFolderPageName(wrapper, "Existing")

      expect(renameSpy).toHaveBeenCalled()
      expect(wrapper.text()).toContain(folderNameConflictMessage)
      expect(folderPageNameEditor(wrapper).text()).toBe("Existing")
      expect(usePopups().popups.peek()).toHaveLength(0)

      wrapper.unmount()
    })

    it("does not offer a second rename form in Settings", async () => {
      const { wrapper } = await mountFolderPageReady(router, 10, "Original")

      await openFolderSettingsTab(wrapper)

      const settings = wrapper.get('[data-testid="folder-settings"]')
      expect(settings.text()).not.toContain("Folder name")
      expect(settings.text()).not.toContain("Rename folder")

      wrapper.unmount()
    })
  })

  describe("dissolve", () => {
    it("soft-deleted shows inline error; name conflict confirms merge and retries", async () => {
      const { wrapper } = mountFolderPage(router, 20, "Mid")

      const dissolveSpy = vi
        .spyOn(NotebookController, "dissolveFolder")
        .mockResolvedValue(
          wrapSdkError({
            status: 409,
            errorType: "SOFT_DELETED_TITLE_CONFLICT",
            message: softDeletedTitleConflictMessage,
          })
        )

      await dissolveWithInitialConfirm(wrapper)
      expect(wrapper.text()).toContain(softDeletedTitleConflictMessage)

      dissolveSpy.mockResolvedValue(
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

      expect(dissolveSpy).toHaveBeenLastCalledWith(
        expect.objectContaining({ query: { merge: true } })
      )

      wrapper.unmount()
    })
  })
})
