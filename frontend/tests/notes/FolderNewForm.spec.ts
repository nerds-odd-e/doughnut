import FolderNewForm from "@/components/notes/FolderNewForm.vue"
import { flushPromises } from "@vue/test-utils"
import helper, { mockSdkService } from "@tests/helpers"
import { describe, it, expect, beforeEach, vi } from "vitest"

describe("FolderNewForm", () => {
  beforeEach(() => {
    vi.resetAllMocks()
    mockSdkService("listNotebookFolderIndex", [])
    mockSdkService("listNotebookFolderListing", { folders: [] })
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
})
