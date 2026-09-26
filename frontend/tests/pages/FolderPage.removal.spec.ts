import { NotebookFolderController } from "@generated/donut-backend-api/sdk.gen"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import type { Router } from "vue-router"
import {
  mockSdkServiceWithImplementation,
  testFolderStub,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  createFolderPageRouter,
  type MountFolderPageOptions,
  mountFolderPageReady,
  openFolderSettingsTab,
  resolveTopConfirm,
  stubRouterPush,
} from "./folderPageTestSupport"

const trashRoot = () => testFolderStub(1, "_trash")
const toBiologyFolder = (notebookId: number) => ({
  name: "folderPage",
  params: { notebookId: String(notebookId), folderId: "10" },
})
const toNotebookPage = (notebookId: number) => ({
  name: "notebookPage",
  params: { notebookId },
})
const topConfirmation = () => usePopups().popups.peek()?.[0]

const action = (wrapper: VueWrapper, testId: string) =>
  wrapper.find(`[data-testid="${testId}"]`)

async function clickSettingsAction(
  wrapper: VueWrapper,
  testId: string,
  confirmed: boolean
) {
  await openFolderSettingsTab(wrapper)
  await action(wrapper, testId).trigger("click")
  resolveTopConfirm(confirmed)
  await flushPromises()
}

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage removal", () => {
  let router: Router

  beforeEach(() => {
    router = createFolderPageRouter()
  })

  const mountTopic = (options?: MountFolderPageOptions) =>
    mountFolderPageReady(router, 20, "Topic", options)

  it("dissolve confirms merge on name conflict and retries", async () => {
    const { wrapper } = await mountFolderPageReady(router, 20, "Mid")
    const dissolveSpy = vi
      .spyOn(NotebookFolderController, "dissolveFolder")
      .mockResolvedValue(
        wrapSdkError({
          status: 409,
          errorType: "FOLDER_NAME_CONFLICT",
          message:
            "A folder with this name already exists at the destination: Inner",
        })
      )

    await clickSettingsAction(wrapper, "folder-dissolve-button", true)

    expect(topConfirmation()?.type).toBe("confirm")
    expect(topConfirmation()?.message).toContain("Merge them?")

    dissolveSpy.mockResolvedValueOnce(wrapSdkResponse(undefined) as never)
    resolveTopConfirm(true)
    await flushPromises()

    expect(dissolveSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ query: { merge: true } })
    )
    wrapper.unmount()
  })

  describe("trash", () => {
    it("cancels without calling the trash action", async () => {
      const { wrapper } = await mountTopic()
      const trashSpy = vi.spyOn(NotebookFolderController, "trashFolder")
      await openFolderSettingsTab(wrapper)

      const consequence =
        "Its contents leave active use. References remain authored, but may no longer resolve until the folder is recovered with Move."
      expect(
        action(wrapper, "folder-trash-button").element.previousElementSibling
          ?.textContent
      ).toBe(`Trash folder "Topic" with its complete subtree. ${consequence}`)
      expect(action(wrapper, "folder-permanent-delete-button").exists()).toBe(
        false
      )

      await action(wrapper, "folder-trash-button").trigger("click")
      expect(topConfirmation()?.type).toBe("confirm")
      expect(topConfirmation()?.message).toBe(
        `Trash folder "Topic" with its complete subtree? ${consequence}`
      )
      resolveTopConfirm(false)
      await flushPromises()

      expect(trashSpy).not.toHaveBeenCalled()
      wrapper.unmount()
    })

    it.each([
      {
        label: "the former parent",
        ancestorFolders: [testFolderStub(10, "Biology")],
        route: toBiologyFolder,
      },
      {
        label: "the notebook page from a root folder",
        ancestorFolders: [],
        route: toNotebookPage,
      },
    ])(
      "trashes and navigates to $label",
      async ({ ancestorFolders, route }) => {
        const { wrapper, folderRealm } = await mountTopic({ ancestorFolders })
        const push = stubRouterPush(router)
        vi.spyOn(NotebookFolderController, "trashFolder").mockResolvedValue(
          wrapSdkResponse(folderRealm.folder)
        )

        await clickSettingsAction(wrapper, "folder-trash-button", true)

        expect(push).toHaveBeenCalledWith(
          route(folderRealm.notebookRealm.notebook.id)
        )
        wrapper.unmount()
      }
    )

    it("disables folder actions while trash is loading", async () => {
      const { wrapper, folderRealm } = await mountTopic()
      let finishTrash!: () => void
      mockSdkServiceWithImplementation(
        NotebookFolderController,
        "trashFolder",
        () =>
          new Promise((resolve) => {
            finishTrash = () => resolve(folderRealm.folder)
          })
      )

      await clickSettingsAction(wrapper, "folder-trash-button", true)

      expect(
        action(wrapper, "folder-trash-button").attributes("disabled")
      ).toBeDefined()
      expect(
        action(wrapper, "folder-move-submit").attributes("disabled")
      ).toBeDefined()

      finishTrash()
      await flushPromises()
      wrapper.unmount()
    })
  })

  describe("permanent deletion", () => {
    it("cancels without calling the permanent delete action", async () => {
      const { wrapper } = await mountTopic({
        ancestorFolders: [trashRoot()],
      })
      const deleteSpy = vi.spyOn(
        NotebookFolderController,
        "permanentlyDeleteFolder"
      )
      await openFolderSettingsTab(wrapper)

      await action(wrapper, "folder-permanent-delete-button").trigger("click")
      expect(topConfirmation()?.type).toBe("confirm")
      for (const phrase of [
        'folder "Topic"',
        "everything inside it",
        "learning history, questions, conversations and images",
        "cannot be undone",
        "Earlier Git history of this notebook still contains the text",
      ]) {
        expect(topConfirmation()?.message).toContain(phrase)
      }
      resolveTopConfirm(false)
      await flushPromises()

      expect(deleteSpy).not.toHaveBeenCalled()
      wrapper.unmount()
    })

    it.each([
      {
        label: "a folder in trash to its former parent",
        id: 20,
        name: "Topic",
        ancestorFolders: [trashRoot(), testFolderStub(10, "Biology")],
        route: toBiologyFolder,
      },
      {
        label: "the trash root itself to the notebook page",
        id: 1,
        name: "_TrAsH",
        ancestorFolders: [],
        route: toNotebookPage,
      },
    ])(
      "permanently deletes $label, offering no Trash within trash",
      async ({ id, name, ancestorFolders, route }) => {
        const { wrapper, folderRealm } = await mountFolderPageReady(
          router,
          id,
          name,
          { ancestorFolders }
        )
        const push = stubRouterPush(router)
        vi.spyOn(
          NotebookFolderController,
          "permanentlyDeleteFolder"
        ).mockResolvedValue(wrapSdkResponse(undefined))

        await openFolderSettingsTab(wrapper)
        expect(action(wrapper, "folder-trash-button").exists()).toBe(false)
        await clickSettingsAction(
          wrapper,
          "folder-permanent-delete-button",
          true
        )

        expect(push).toHaveBeenCalledWith(
          route(folderRealm.notebookRealm.notebook.id)
        )
        wrapper.unmount()
      }
    )
  })
})
