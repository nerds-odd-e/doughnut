import makeMe from "doughnut-test-fixtures/makeMe"
import NotebookPageWithNotebookSidebarLayout from "@tests/fixtures/NotebookPageWithNotebookSidebarLayout.vue"
import helper, {
  mockSdkService,
  mockSdkServiceWithImplementation,
} from "@tests/helpers"
import { resetNotebookSidebarState } from "@/composables/useCurrentNoteSidebarState"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect } from "vitest"

describe("NotebookPage.spec", () => {
  beforeEach(() => {
    resetNotebookSidebarState()
    mockSdkService("getApprovalForNotebook", { approval: undefined })
    mockSdkService("getAiAssistant", { additionalInstructionsToAi: "" })
  })

  it("shows the current number of questions in assessment if set", async () => {
    const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
    mockSdkService("get", { notebook, hasAttachedBook: false, readonly: false })
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
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

  it("shows the note-show sidebar toggle and loads index state when notebook exposes landing id", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm.title("index").please()
    indexRealm.notebookView = { notebook, readonly: false }
    mockSdkService("get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
      indexNoteId: indexRealm.id,
    })
    const showSpy = mockSdkService("showNote", indexRealm)
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookPage",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    expect(
      screen.getByRole("button", { name: /hide sidebar|show sidebar/i })
    ).toBeInTheDocument()
    expect(showSpy).toHaveBeenCalledWith({
      path: { note: indexRealm.id },
    })
    const edit = screen.getByTestId("notebook-index-edit")
    const toAttr = edit.getAttribute("to")
    expect(toAttr).toBeTruthy()
    expect(JSON.parse(toAttr!)).toEqual(
      expect.objectContaining({
        name: "noteShow",
        params: expect.objectContaining({
          noteId: String(indexRealm.id),
        }),
      })
    )
  })

  it("does not show empty-index prompt while landing note load is delayed", async () => {
    const notebook = makeMe.aNotebook.please()
    const indexRealm = makeMe.aNoteRealm.title("index").please()
    indexRealm.notebookView = { notebook, readonly: false }
    mockSdkService("get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
      indexNoteId: indexRealm.id,
    })
    let release!: () => void
    const gate = new Promise<void>((resolve) => {
      release = resolve
    })
    mockSdkServiceWithImplementation("showNote", async () => {
      await gate
      return indexRealm
    })
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
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

  it("shows add-first-note prompt when notebook omits landing index id", async () => {
    const notebook = makeMe.aNotebook.please()
    mockSdkService("get", {
      notebook,
      hasAttachedBook: false,
      readonly: false,
    })
    helper
      .component(NotebookPageWithNotebookSidebarLayout)
      .withCleanStorage()
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookPage",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    expect(
      screen.getByRole("button", { name: /hide sidebar|show sidebar/i })
    ).toBeInTheDocument()
    expect(screen.getByTestId("notebook-add-first-note")).toBeInTheDocument()
  })
})
