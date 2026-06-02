import {
  NoteController,
  NotebookController,
  SearchController,
} from "@generated/doughnut-backend-api/sdk.gen"
import type { User } from "@generated/doughnut-backend-api"
import makeMe from "doughnut-test-fixtures/makeMe"
import NotebookPageWithNotebookSidebarLayout from "@tests/fixtures/NotebookPageWithNotebookSidebarLayout.vue"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { useGlobalKeyboardShortcuts } from "@/composables/useGlobalKeyboardShortcuts"
import { screen } from "@testing-library/vue"
import userEvent from "@testing-library/user-event"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"
import { createRouter, createWebHistory } from "vue-router"
import routes from "@/routes/routes"
import { defineComponent, h, provide, ref } from "vue"

function dispatchNoteNewShortcut(modifiers: KeyboardEventInit) {
  document.dispatchEvent(
    new KeyboardEvent("keydown", {
      key: "n",
      code: "KeyN",
      bubbles: true,
      cancelable: true,
      ...modifiers,
    })
  )
}

function mockNoteNewFormServices() {
  mockSdkService(NoteController, "getRecentNotes", [])
  mockSdkService(SearchController, "searchForRelationshipTarget", [])
  mockSdkService(SearchController, "searchForRelationshipTargetWithin", [])
  mockSdkService(SearchController, "semanticSearch", [])
  mockSdkService(SearchController, "semanticSearchWithin", [])
  mockSdkService(NotebookController, "listNotebookFolderIndex", [])
  mockSdkService(NotebookController, "listNotebookFolderListing", {
    folders: [],
  })
}

describe("NotebookPage.spec", () => {
  let router: ReturnType<typeof createRouter>

  function renderNotebookPageWithKeyboardShortcuts(loggedInUser?: User) {
    const userRef = ref(loggedInUser)
    const Harness = defineComponent({
      setup() {
        provide("currentUser", userRef)
        useGlobalKeyboardShortcuts(userRef)
        return () => h(NotebookPageWithNotebookSidebarLayout)
      },
    })
    return helper
      .component(Harness)
      .withCleanStorage()
      .withRouter(router)
      .render()
  }

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
    await userEvent.keyboard("New notebook index")

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

  it.each([
    { label: "Ctrl+N", modifiers: { ctrlKey: true } },
    { label: "Cmd+N", modifiers: { metaKey: true } },
  ])("opens new note form on $label when logged in", async ({ modifiers }) => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
    })
    mockNoteNewFormServices()
    const { unmount } = renderNotebookPageWithKeyboardShortcuts(
      makeMe.aUser.please()
    )
    await navigateToNotebookPage(notebook.id)

    expect(screen.queryByTestId("note-new-form")).toBeNull()
    dispatchNoteNewShortcut(modifiers)
    expect(await screen.findByTestId("note-new-form")).toBeInTheDocument()
    unmount()
  })

  it("does not open new note form on Ctrl+N when logged out", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
    })
    mockNoteNewFormServices()
    const { unmount } = renderNotebookPageWithKeyboardShortcuts()
    await navigateToNotebookPage(notebook.id)

    dispatchNoteNewShortcut({ ctrlKey: true })
    expect(screen.queryByTestId("note-new-form")).toBeNull()
    unmount()
  })
})
