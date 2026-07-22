import { describe, expect, it } from "vitest"
import {
  createFolderPageRouter,
  mountFolderPageReady,
} from "./folderPageTestSupport"

describe("FolderPage Health tab", () => {
  it("shows Readme and Settings tabs but not Health", async () => {
    const router = createFolderPageRouter()
    const { wrapper } = await mountFolderPageReady(router, 1, "Folder Root")

    expect(
      wrapper.find('[data-testid="folder-workspace-tab-readme"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="folder-workspace-tab-settings"]').exists()
    ).toBe(true)
    expect(
      wrapper.find('[data-testid="folder-workspace-tab-health"]').exists()
    ).toBe(false)
  })
})
