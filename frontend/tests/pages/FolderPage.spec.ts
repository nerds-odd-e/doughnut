import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import FolderPage from "@/pages/FolderPage.vue"
import { flushPromises } from "@vue/test-utils"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  wrapSdkError,
  wrapSdkResponse,
} from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

afterEach(() => {
  document.body.innerHTML = ""
  vi.restoreAllMocks()
})

describe("FolderPage move", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({ history: createWebHistory(), routes })
    mockSdkService(NotebookController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
  })

  function mountFolderPage(folderId = 10, folderName = "Dup") {
    const folderRealm = makeMe.aFolderRealm
      .folder(folderId, folderName)
      .please()
    const wrapper = helper
      .component(FolderPage)
      .withCleanStorage()
      .withRouter(router)
      .withProps({
        folderRealm,
        fetchFolderPage: vi.fn().mockResolvedValue(undefined),
      })
      .mount({ attachTo: document.body })
    return { wrapper, folderRealm }
  }

  async function submitMoveForm(
    wrapper: ReturnType<typeof mountFolderPage>["wrapper"]
  ) {
    await wrapper
      .find('[data-testid="folder-move-dialog"] form')
      .trigger("submit")
    await flushPromises()
  }

  it("shows a merge confirm when move returns 409 and retries with merge flag", async () => {
    const { wrapper, folderRealm } = mountFolderPage()
    await flushPromises()

    const moveSpy = vi
      .spyOn(NotebookController, "moveFolder")
      .mockResolvedValue(
        wrapSdkError({
          status: 409,
          message: "A folder with this name already exists here.",
        })
      )

    await submitMoveForm(wrapper)

    const popup = usePopups().popups.peek()?.[0]
    expect(popup?.type).toBe("confirm")
    expect(popup?.message).toContain("Merge into it?")

    moveSpy.mockResolvedValueOnce(wrapSdkResponse(folderRealm.folder) as never)
    usePopups().popups.done(true)
    await flushPromises()

    expect(moveSpy).toHaveBeenCalledTimes(2)
    expect(moveSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({
        body: expect.objectContaining({ merge: true }),
      })
    )

    wrapper.unmount()
  })

  it("shows error message when move 409 and user cancels merge", async () => {
    const { wrapper } = mountFolderPage()
    await flushPromises()

    vi.spyOn(NotebookController, "moveFolder").mockResolvedValue(
      wrapSdkError({
        status: 409,
        message: "A folder with this name already exists here.",
      })
    )

    await submitMoveForm(wrapper)

    usePopups().popups.done(false)
    await flushPromises()

    expect(wrapper.text()).toContain(
      "A folder with this name already exists here."
    )
    wrapper.unmount()
  })
})

describe("FolderPage dissolve", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({ history: createWebHistory(), routes })
    mockSdkService(NotebookController, "listNotebookFolderIndex", [])
    mockSdkService(NotebookController, "listNotebookFolderListing", {
      folders: [],
    })
  })

  function mountFolderPage(folderId = 20, folderName = "Mid") {
    const folderRealm = makeMe.aFolderRealm
      .folder(folderId, folderName)
      .please()
    const wrapper = helper
      .component(FolderPage)
      .withCleanStorage()
      .withRouter(router)
      .withProps({
        folderRealm,
        fetchFolderPage: vi.fn().mockResolvedValue(undefined),
      })
      .mount({ attachTo: document.body })
    return { wrapper, folderRealm }
  }

  it("shows merge confirm when dissolve returns 409 and retries with merge=true", async () => {
    const { wrapper } = mountFolderPage()
    await flushPromises()

    const dissolveSpy = vi
      .spyOn(NotebookController, "dissolveFolder")
      .mockResolvedValue(
        wrapSdkError({
          status: 409,
          message:
            "A folder with this name already exists at the destination: Inner",
        })
      )

    const dissolveBtn = wrapper.find('[data-testid="folder-dissolve-button"]')

    dissolveBtn.trigger("click")
    await flushPromises()
    usePopups().popups.done(true)
    await flushPromises()

    const mergePopup = usePopups().popups.peek()?.[0]
    expect(mergePopup?.type).toBe("confirm")
    expect(mergePopup?.message).toContain("Merge them?")

    dissolveSpy.mockResolvedValueOnce(wrapSdkResponse(undefined) as never)
    usePopups().popups.done(true)
    await flushPromises()

    expect(dissolveSpy).toHaveBeenCalledTimes(2)
    expect(dissolveSpy).toHaveBeenLastCalledWith(
      expect.objectContaining({ query: { merge: true } })
    )

    wrapper.unmount()
  })
})
