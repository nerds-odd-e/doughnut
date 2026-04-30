import makeMe from "doughnut-test-fixtures/makeMe"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
  wrapSdkError,
} from "@tests/helpers"
import NotebookPage from "@/pages/NotebookPage.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"

describe("NotebookPage.spec", () => {
  beforeEach(() => {
    mockSdkService("getApprovalForNotebook", { approval: undefined })
    mockSdkService("getAiAssistant", { additionalInstructionsToAi: "" })
  })

  it("shows the current number of questions in assessment if set", async () => {
    const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
    mockSdkService("get", { notebook, hasAttachedBook: false })
    vi.spyOn(NotebookController, "getNoteBySlug").mockResolvedValue(
      wrapSdkError("no index") as never
    )
    helper
      .component(NotebookPage)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookPage",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    const input = screen.getByLabelText(/Number Of Questions In Assessment/i)
    expect((input as HTMLInputElement).value).toBe("4")
  })

  it("shows the note-show sidebar toggle and loads the tree via index slug when present", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm.title("index").please()
    indexRealm.notebookId = notebook.id
    indexRealm.note.noteTopology.notebookId = notebook.id
    mockSdkService("get", { notebook, hasAttachedBook: false })
    const slugSpy = mockSdkService("getNoteBySlug", indexRealm)
    helper
      .component(NotebookPage)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookPage",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    expect(screen.getByTitle("toggle sidebar")).toBeInTheDocument()
    expect(slugSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { notebook: notebook.id },
        query: { slugPath: "index" },
      })
    )
    const edit = screen.getByTestId("notebook-index-edit")
    const toAttr = edit.getAttribute("to")
    expect(toAttr).toBeTruthy()
    expect(JSON.parse(toAttr!)).toEqual(
      expect.objectContaining({
        name: "noteShow",
        params: expect.objectContaining({
          notebookId: String(notebook.id),
          noteSlugPath: "index",
        }),
      })
    )
  })

  it("does not show empty-index prompt while index slug is still loading", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm.title("index").please()
    indexRealm.notebookId = notebook.id
    indexRealm.note.noteTopology.notebookId = notebook.id
    mockSdkService("get", { notebook, hasAttachedBook: false })
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    mockSdkServiceWithImplementation("getNoteBySlug", async () => {
      await gate
      return indexRealm
    })
    helper
      .component(NotebookPage)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookPage",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    expect(screen.queryByTestId("notebook-add-first-note")).toBeNull()
    expect(screen.getByTestId("notebook-index-loading")).toBeInTheDocument()
    release()
    await flushPromises()
    expect(screen.queryByTestId("notebook-index-loading")).toBeNull()
    expect(screen.queryByTestId("notebook-add-first-note")).toBeNull()
    expect(screen.getByTestId("notebook-index-edit")).toBeInTheDocument()
  })

  it("does not load sidebar via showNote when index slug is missing", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService("get", { notebook, hasAttachedBook: false })
    vi.spyOn(NotebookController, "getNoteBySlug").mockResolvedValue(
      wrapSdkError("not found") as never
    )
    const showSpy = mockSdkService("showNote", makeMe.aNoteRealm.please())
    helper
      .component(NotebookPage)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookPage",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    expect(screen.getByTitle("toggle sidebar")).toBeInTheDocument()
    expect(showSpy).not.toHaveBeenCalled()
    expect(screen.getByTestId("notebook-add-first-note")).toBeInTheDocument()
  })
})
