import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { Router } from "vue-router"
import {
  mockSdkServiceWithImplementation,
  testFolderStub,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  mountFolderPageReady,
  openFolderSettingsTab,
  resolveTopConfirm,
  stubRouterPush,
} from "@tests/pages/folderPageTestSupport"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage trash", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  it("cancels without calling the trash action", async () => {
    const { wrapper } = await mountFolderPageReady(router, 20, "Topic")
    const trashSpy = vi.spyOn(NotebookController, "trashFolder")
    await openFolderSettingsTab(wrapper)

    const trashButton = wrapper.get('[data-testid="folder-trash-button"]')
    const consequence =
      "Its contents leave active use. References remain authored, but may no longer resolve until the folder is recovered with Move."
    expect(trashButton.element.previousElementSibling?.textContent).toBe(
      `Trash folder "Topic" with its complete subtree. ${consequence}`
    )

    await trashButton.trigger("click")
    const confirmation = usePopups().popups.peek()?.[0]
    expect(confirmation?.type).toBe("confirm")
    expect(confirmation?.message).toBe(
      `Trash folder "Topic" with its complete subtree? ${consequence}`
    )
    resolveTopConfirm(false)
    await flushPromises()

    expect(trashSpy).not.toHaveBeenCalled()
    wrapper.unmount()
  })

  it("trashes and navigates to the former parent", async () => {
    const parent = testFolderStub(10, "Biology")
    const { wrapper, folderRealm } = await mountFolderPageReady(
      router,
      20,
      "Topic",
      { ancestorFolders: [parent] }
    )
    const push = stubRouterPush(router)
    vi.spyOn(NotebookController, "trashFolder").mockResolvedValue(
      wrapSdkResponse(folderRealm.folder)
    )
    await openFolderSettingsTab(wrapper)

    await wrapper.get('[data-testid="folder-trash-button"]').trigger("click")
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

  it("disables folder actions while trash is loading", async () => {
    const { wrapper, folderRealm } = await mountFolderPageReady(
      router,
      20,
      "Topic"
    )
    let finishTrash!: () => void
    mockSdkServiceWithImplementation(
      NotebookController,
      "trashFolder",
      () =>
        new Promise((resolve) => {
          finishTrash = () => resolve(folderRealm.folder)
        })
    )
    await openFolderSettingsTab(wrapper)

    await wrapper.get('[data-testid="folder-trash-button"]').trigger("click")
    resolveTopConfirm(true)
    await flushPromises()

    expect(
      wrapper.get('[data-testid="folder-trash-button"]').attributes("disabled")
    ).toBeDefined()
    expect(
      wrapper.get('[data-testid="folder-move-submit"]').attributes("disabled")
    ).toBeDefined()

    finishTrash()
    await flushPromises()
    wrapper.unmount()
  })

  it("navigates to notebook root and hides Trash within trash", async () => {
    const rootMounted = await mountFolderPageReady(router, 20, "Topic")
    const rootPush = stubRouterPush(router)
    vi.spyOn(NotebookController, "trashFolder").mockResolvedValue(
      wrapSdkResponse(rootMounted.folderRealm.folder)
    )
    await openFolderSettingsTab(rootMounted.wrapper)
    await rootMounted.wrapper
      .get('[data-testid="folder-trash-button"]')
      .trigger("click")
    resolveTopConfirm(true)
    await flushPromises()
    expect(rootPush).toHaveBeenCalledWith({
      name: "notebookPage",
      params: { notebookId: rootMounted.folderRealm.notebookRealm.notebook.id },
    })
    rootMounted.wrapper.unmount()

    const trash = testFolderStub(1, "_trash")
    const trashedMounted = await mountFolderPageReady(router, 21, "Topic", {
      ancestorFolders: [trash],
    })
    await openFolderSettingsTab(trashedMounted.wrapper)
    expect(
      trashedMounted.wrapper
        .find('[data-testid="folder-trash-button"]')
        .exists()
    ).toBe(false)
    trashedMounted.wrapper.unmount()

    const trashRootMounted = await mountFolderPageReady(router, 1, "_TrAsH")
    await openFolderSettingsTab(trashRootMounted.wrapper)
    expect(
      trashRootMounted.wrapper
        .find('[data-testid="folder-trash-button"]')
        .exists()
    ).toBe(false)
    trashRootMounted.wrapper.unmount()
  })
})
