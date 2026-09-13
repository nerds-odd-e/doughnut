import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { Router } from "vue-router"
import {
  createFolderPageRouter,
  mountFolderPageReady,
} from "./folderPageTestSupport"
import { testFolderStub } from "@tests/helpers"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage trash warning", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  it("warns when the current folder is the notebook-root _trash", async () => {
    const { wrapper } = await mountFolderPageReady(router, 1, "_trash", {
      ancestorFolders: [],
    })

    expect(
      wrapper.find('[data-testid="folder-availability-warning"]').exists()
    ).toBe(true)
    expect(
      wrapper.get('[data-testid="folder-availability-warning"]').text()
    ).toBe("This folder is in trash")
    wrapper.unmount()
  })

  it("warns for a descendant folder beneath _trash", async () => {
    const { wrapper } = await mountFolderPageReady(router, 2, "Biology", {
      ancestorFolders: [testFolderStub(1, "_trash")],
    })

    expect(
      wrapper.find('[data-testid="folder-availability-warning"]').exists()
    ).toBe(true)
    wrapper.unmount()
  })

  it("matches the trash root case-insensitively", async () => {
    const { wrapper } = await mountFolderPageReady(router, 1, "_TrAsH", {
      ancestorFolders: [],
    })

    expect(
      wrapper.find('[data-testid="folder-availability-warning"]').exists()
    ).toBe(true)
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
