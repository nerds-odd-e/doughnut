import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import FolderNewForm from "@/components/notes/FolderNewForm.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService, testFolderStub } from "@tests/helpers"
import { describe, it, expect, beforeEach, vi } from "vitest"

const routerPush = vi.fn()

vi.mock("vue-router", async (importOriginal) => {
  const actual = await importOriginal<typeof import("vue-router")>()
  return {
    ...actual,
    useRouter: () => ({
      push: routerPush,
    }),
  }
})

describe("FolderNewForm", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    routerPush.mockResolvedValue(undefined)
    mockSdkService(NotebookController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
  })

  it("includes FolderSelector for parent folder", async () => {
    const wrapper = helper
      .component(FolderNewForm)
      .withCleanStorage()
      .withProps({
        notebookId: 301,
        ancestorFolders: [],
        contextFolderId: null,
        initialParentFolderId: null,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    expect(wrapper.find('[data-testid="folder-new-dialog"]').exists()).toBe(
      true
    )
    expect(wrapper.text()).toContain("Parent folder")
    expect(
      wrapper.find('[data-testid="folder-move-parent-select"]').exists()
    ).toBe(true)

    wrapper.unmount()
  })

  it("navigates to the new folder page after successful create", async () => {
    const createFolderSpy = mockSdkService(
      NotebookController,
      "createFolder",
      testFolderStub(901, "New Folder")
    )
    const wrapper = helper
      .component(FolderNewForm)
      .withCleanStorage()
      .withProps({
        notebookId: 301,
        ancestorFolders: [],
        contextFolderId: null,
        initialParentFolderId: null,
      })
      .mount({ attachTo: document.body })

    await flushPromises()

    const nameInput = wrapper.find(".seamless-editor").element as HTMLElement
    nameInput.innerText = "New Folder"
    nameInput.dispatchEvent(new Event("input", { bubbles: true }))
    await flushPromises()

    await wrapper
      .find('[data-testid="folder-new-dialog-submit"]')
      .trigger("click")

    await flushPromises()

    expect(createFolderSpy).toHaveBeenCalled()
    expect(routerPush).toHaveBeenCalledWith({
      name: "folderPage",
      params: {
        notebookId: "301",
        folderId: "901",
      },
    })

    wrapper.unmount()
  })
})
