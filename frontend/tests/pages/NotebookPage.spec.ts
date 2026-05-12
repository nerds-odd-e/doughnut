import {
  NoteController,
  NotebookController,
} from "@generated/doughnut-backend-api/sdk.gen"
import makeMe from "doughnut-test-fixtures/makeMe"
import NotebookPageWithNotebookSidebarLayout from "@tests/fixtures/NotebookPageWithNotebookSidebarLayout.vue"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { screen, waitFor } from "@testing-library/vue"
import userEvent from "@testing-library/user-event"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect } from "vitest"
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

  it("shows the sidebar toggle and loads index state when notebook exposes landing id", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm.title("index").please()
    indexRealm.notebookRealm = { notebook, readonly: false }
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
      indexNoteId: indexRealm.id,
    })
    const showSpy = mockSdkService(NoteController, "showNote", indexRealm)
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)
    expect(screen.getByLabelText("Show sidebar")).toBeInTheDocument()
    expect(showSpy).toHaveBeenCalledWith({
      path: { note: indexRealm.id },
    })
    expect(
      screen.getByTestId("notebook-index-editable-content")
    ).toBeInTheDocument()
  })

  it("does not show absent index editor while landing note load is delayed", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm.title("index").please()
    indexRealm.notebookRealm = { notebook, readonly: false }
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
      indexNoteId: indexRealm.id,
    })
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    mockSdkServiceWithImplementation(NoteController, "showNote", async () => {
      await gate
      return indexRealm
    })
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)
    expect(screen.queryByTestId("notebook-index-draft-editor")).toBeNull()
    expect(screen.getByTestId("notebook-index-loading")).toBeInTheDocument()
    release()
    await flushPromises()
    expect(screen.queryByTestId("notebook-index-loading")).toBeNull()
    expect(screen.queryByTestId("notebook-index-draft-editor")).toBeNull()
    expect(
      screen.getByTestId("notebook-index-editable-content")
    ).toBeInTheDocument()
  })

  it("shows index draft editor when notebook omits landing index id", async () => {
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
    expect(screen.getByLabelText("Show sidebar")).toBeInTheDocument()
    expect(
      screen.getByTestId("notebook-index-draft-editor")
    ).toBeInTheDocument()
    expect(screen.getByTestId("notebook-index-create-save")).toBeInTheDocument()
  })

  it("with existing index does not call createNoteAtNotebookRoot on load", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm
      .title("index")
      .content("existing")
      .please()
    indexRealm.notebookRealm = { notebook, readonly: false }
    mockSdkService(NotebookController, "get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
      indexNoteId: indexRealm.id,
    })
    mockSdkService(NoteController, "showNote", indexRealm)
    const createSpy = mockSdkService(
      NotebookController,
      "createNoteAtNotebookRoot",
      indexRealm
    )

    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)

    expect(
      screen.getByTestId("notebook-index-editable-content")
    ).toBeInTheDocument()
    expect(createSpy).not.toHaveBeenCalled()
  })

  it("creates root index note on save when none exists then shows inline editor", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm
      .title("index")
      .content("Notebook index body")
      .please()
    indexRealm.notebookRealm = { notebook, readonly: false }

    let getCount = 0
    mockSdkServiceWithImplementation(NotebookController, "get", async () => {
      getCount += 1
      if (getCount === 1) {
        return {
          notebook,
          hasAttachedBook: false,
          readonly: false,
        }
      }
      return {
        notebook,
        hasAttachedBook: false,
        readonly: false,
        indexNoteId: indexRealm.id,
      }
    })

    const createSpy = mockSdkServiceWithImplementation(
      NotebookController,
      "createNoteAtNotebookRoot",
      async (opts) => {
        expect(opts.path?.notebook).toBe(notebook.id)
        expect(opts.body?.newTitle).toBe("index")
        expect(opts.body?.content ?? "").toMatch(/Typed index text/)
        return indexRealm
      }
    )

    mockSdkService(NoteController, "showNote", indexRealm)

    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter(router)
      .withCurrentUser(makeMe.aUser.please())
      .render()
    await navigateToNotebookPage(notebook.id)

    const rich = document.querySelector(
      '[data-testid="notebook-index-draft-editor"] [data-testid="rich-note-frontmatter-parse-error"]'
    )
    expect(rich).toBeNull()

    const draftRoot = screen.getByTestId("notebook-index-draft-editor")
    const quill = draftRoot.querySelector(".ql-editor") as HTMLElement | null
    expect(quill).toBeTruthy()
    await userEvent.click(quill!)
    await userEvent.keyboard("Typed index text")

    await userEvent.click(screen.getByTestId("notebook-index-create-save"))
    await flushPromises()

    expect(createSpy).toHaveBeenCalledTimes(1)
    await waitFor(() => {
      expect(
        screen.getByTestId("notebook-index-editable-content")
      ).toBeInTheDocument()
    })
    await waitFor(() => {
      expect(getCount).toBeGreaterThanOrEqual(2)
    })
    await flushPromises()
  })
})
