import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import NotebookPageWithNotebookSidebarLayout from "@tests/fixtures/NotebookPageWithNotebookSidebarLayout.vue"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { screen } from "@testing-library/vue"
import userEvent from "@testing-library/user-event"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"

describe("NotebookPage.spec", () => {
  let router: ReturnType<typeof createRouter>

  beforeEach(() => {
    router = createRouter({
      history: createWebHistory(),
      routes,
    })
    mockSdkService(NotebookController, "getAiAssistant", {
      id: 1,
      additionalInstructionsToAi: "",
    })
  })

  async function navigateToNotebookPage(notebookId: number) {
    await router.push({
      name: "notebookPage",
      params: { notebookId: String(notebookId) },
    })
    await flushPromises()
  }

  it("shows New note in main column when sidebar is hidden", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
    })
    const { unmount } = helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)

    const hideSidebar = screen.queryByTitle("Hide sidebar")
    if (hideSidebar != null) {
      expect(screen.queryByTestId("note-main-creation-toolbar")).toBeNull()
      await userEvent.click(hideSidebar)
    }

    expect(screen.getByTestId("note-main-creation-toolbar")).toBeInTheDocument()
    expect(screen.getByTestId("note-creation-new-button")).toBeInTheDocument()
    unmount()
  })

  it("shows index editor when notebook has no indexContent", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
    })
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)
    expect(screen.getByTestId("notebook-index-editor")).toBeInTheDocument()
    expect(screen.getByTestId("notebook-index-save")).toBeInTheDocument()
  })

  it("shows index editor with existing content when notebook has indexContent", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
      indexContent: "# Existing notebook index",
    })
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)
    expect(screen.getByTestId("notebook-index-editor")).toBeInTheDocument()
    expect(screen.getByTestId("notebook-index-save")).toBeInTheDocument()
  })

  it("saves notebook index content directly to container on save", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
    })
    const saveSpy = vi
      .spyOn(NotebookController, "updateNotebookIndexContent")
      .mockResolvedValue(
        wrapSdkResponse({
          notebook,
          hasAttachedBook: false,
          readonly: false,
          indexContent: "New notebook index",
        })
      )

    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)

    const editorRoot = screen.getByTestId("notebook-index-editor")
    const quill = editorRoot.querySelector(".ql-editor") as HTMLElement | null
    expect(quill).toBeTruthy()
    await userEvent.click(quill!)
    await userEvent.paste("New notebook index")

    await userEvent.click(screen.getByTestId("notebook-index-save"))
    await flushPromises()

    expect(saveSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebook.id },
        body: expect.objectContaining({
          content: expect.stringContaining("New notebook index"),
        }),
      })
    )
    saveSpy.mockRestore()
  })
})
