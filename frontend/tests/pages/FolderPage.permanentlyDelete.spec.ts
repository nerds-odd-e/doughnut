import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { Router } from "vue-router"
import { testFolderStub, wrapSdkResponse } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  mountFolderPageReady,
  openFolderSettingsTab,
  resolveTopConfirm,
  stubRouterPush,
} from "@tests/pages/folderPageTestSupport"

const trashRoot = () => testFolderStub(1, "_trash")

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage permanent deletion", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  it("cancels without calling the permanent delete action", async () => {
    const { wrapper } = await mountFolderPageReady(router, 20, "Topic", {
      ancestorFolders: [trashRoot()],
    })
    const deleteSpy = vi.spyOn(
      NotebookFolderController,
      "permanentlyDeleteFolder"
    )
    await openFolderSettingsTab(wrapper)

    await wrapper
      .get('[data-testid="folder-permanent-delete-button"]')
      .trigger("click")
    const confirmation = usePopups().popups.peek()?.[0]
    expect(confirmation?.type).toBe("confirm")
    expect(confirmation?.message).toContain('folder "Topic"')
    expect(confirmation?.message).toContain("everything inside it")
    expect(confirmation?.message).toContain(
      "learning history, questions, conversations and images"
    )
    expect(confirmation?.message).toContain("cannot be undone")
    expect(confirmation?.message).toContain(
      "Earlier Git history of this notebook still contains the text"
    )
    resolveTopConfirm(false)
    await flushPromises()

    expect(deleteSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("permanently deletes and navigates to the former parent", async () => {
    const parent = testFolderStub(10, "Biology")
    const { wrapper, folderRealm } = await mountFolderPageReady(
      router,
      20,
      "Topic",
      { ancestorFolders: [trashRoot(), parent] }
    )
    const push = stubRouterPush(router)
    vi.spyOn(
      NotebookFolderController,
      "permanentlyDeleteFolder"
    ).mockResolvedValue(wrapSdkResponse(undefined))
    await openFolderSettingsTab(wrapper)

    await wrapper
      .get('[data-testid="folder-permanent-delete-button"]')
      .trigger("click")
    resolveTopConfirm(true)
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      name: "folderPage",
      params: {
        notebookId: String(folderRealm.notebookRealm.notebook.id),
        folderId: "10",
      },
    })
    wrapper.unmount()
  })

  it("deletes the trash root itself and navigates to the notebook page", async () => {
    const { wrapper, folderRealm } = await mountFolderPageReady(
      router,
      1,
      "_trash"
    )
    const push = stubRouterPush(router)
    vi.spyOn(
      NotebookFolderController,
      "permanentlyDeleteFolder"
    ).mockResolvedValue(wrapSdkResponse(undefined))
    await openFolderSettingsTab(wrapper)

    await wrapper
      .get('[data-testid="folder-permanent-delete-button"]')
      .trigger("click")
    resolveTopConfirm(true)
    await flushPromises()

    expect(push).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId: folderRealm.notebookRealm.notebook.id },
    })
    wrapper.unmount()
  })

  it("offers no permanent deletion for an active folder", async () => {
    const { wrapper } = await mountFolderPageReady(router, 20, "Topic")
    await openFolderSettingsTab(wrapper)

    expect(
      wrapper.find('[data-testid="folder-permanent-delete-button"]').exists()
    ).toBe(false)
    wrapper.unmount()
  })
})
