import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { Router } from "vue-router"
import { testFolderStub, wrapSdkError, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  editFolderPageName,
  folderNameConflictMessage,
  folderPageNameEditor,
  mountFolderPageReady,
  openFolderSettingsTab,
} from "./folderPageTestSupport"

afterEach(() => {
  document.body.innerHTML = ""
  vi.useRealTimers()
  vi.restoreAllMocks()
})

describe("FolderPage", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  it("shows Readme and Settings tabs but not Health", async () => {
    const { wrapper } = await mountFolderPageReady(router, 1, "Folder Root")

    expect(wrapper.find('[data-testid="folder-tab-readme"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="folder-tab-settings"]').exists()).toBe(
      true
    )
    expect(wrapper.find('[data-testid="folder-tab-health"]').exists()).toBe(
      false
    )
  })

  describe("trash warning", () => {
    it.each([
      { label: "the notebook-root _trash", name: "_trash", ancestors: [] },
      {
        label: "a descendant folder beneath _trash",
        name: "Biology",
        ancestors: ["_trash"],
      },
      {
        label: "the trash root matched case-insensitively",
        name: "_TrAsH",
        ancestors: [],
      },
    ])("warns for $label", async ({ name, ancestors }) => {
      const { wrapper } = await mountFolderPageReady(router, 2, name, {
        ancestorFolders: ancestors.map((n) => testFolderStub(1, n)),
      })

      expect(
        wrapper.get('[data-testid="folder-availability-warning"]').text()
      ).toBe("This folder is in trash")
      wrapper.unmount()
    })

    it("does not warn for an active Projects/_trash folder", async () => {
      const { wrapper } = await mountFolderPageReady(router, 2, "_trash", {
        ancestorFolders: [testFolderStub(1, "Projects")],
      })

      expect(
        wrapper.find('[data-testid="folder-availability-warning"]').exists()
      ).toBe(false)
      wrapper.unmount()
    })
  })

  describe("rename", () => {
    async function replacePendingFolderNameWith(finalName: string) {
      vi.useFakeTimers()
      const { wrapper } = await mountFolderPageReady(router, 10, "Original")
      const renameSpy = vi
        .spyOn(NotebookFolderController, "renameFolder")
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
        .spyOn(NotebookFolderController, "renameFolder")
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
        .spyOn(NotebookFolderController, "renameFolder")
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
})
