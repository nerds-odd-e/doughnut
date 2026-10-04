import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import FolderNewForm from "@/components/notes/FolderNewForm.vue"
import { flushPromises } from "@vue/test-utils"
import helper, {
  mockSdkService,
  productionRouterAt,
  testFolderStub,
} from "@tests/helpers"
import { describe, it, expect, beforeEach, vi } from "vitest"

describe("FolderNewForm", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockSdkService(NotebookFolderController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookFolderController, "listNotebookFolderListing", {
      folders: [],
    })
  })

  it("includes FolderSelector for parent folder", async () => {
    const wrapper = helper
      .component(FolderNewForm)
      .withCleanStorage()
      .withRouter()
      .withProps({
        notebookId: 301,
        ancestorFolders: [],
        contextFolder: null,
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
      NotebookFolderController,
      "createFolder",
      testFolderStub(901, "New Folder")
    )
    const router = await productionRouterAt({ name: "root" })
    const wrapper = helper
      .component(FolderNewForm)
      .withCleanStorage()
      .withRouter(router)
      .withProps({
        notebookId: 301,
        ancestorFolders: [],
        contextFolder: null,
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
    expect(router.currentRoute.value).toMatchObject({
      name: "folderPage",
      params: {
        notebookId: "301",
        folderId: "901",
      },
    })

    wrapper.unmount()
  })
})
