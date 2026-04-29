import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, wrapSdkError } from "@tests/helpers"
import NotebookPage from "@/pages/NotebookPage.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect, vi } from "vitest"
import type { NoteRealm, Notebook } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"

function noteRealmForNotebookHead(notebook: Notebook): NoteRealm {
  const headId = notebook.headNoteId
  const r = makeMe.aNoteRealm.please()
  return {
    ...r,
    id: headId,
    note: {
      ...r.note,
      id: headId,
      noteTopology: {
        ...r.note.noteTopology,
        id: headId,
        notebookId: notebook.id,
      },
    },
    notebook: { ...notebook },
    children: [],
  }
}

describe("NotebookPage.spec", () => {
  beforeEach(() => {
    mockSdkService("getApprovalForNotebook", { approval: undefined })
    mockSdkService("getAiAssistant", { additionalInstructionsToAi: "" })
  })

  it("shows the current number of questions in assessment if set", async () => {
    const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
    mockSdkService("get", notebook)
    vi.spyOn(NotebookController, "getNoteBySlug").mockResolvedValue(
      wrapSdkError("no index") as never
    )
    mockSdkService("showNote", noteRealmForNotebookHead(notebook))
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
    indexRealm.notebook = { ...indexRealm.notebook, id: notebook.id }
    mockSdkService("get", notebook)
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
  })

  it("falls back to head note realm when index slug is missing", async () => {
    const notebook = makeMe.aNotebook.please()
    const headRealm = noteRealmForNotebookHead(notebook)
    mockSdkService("get", notebook)
    vi.spyOn(NotebookController, "getNoteBySlug").mockResolvedValue(
      wrapSdkError("not found") as never
    )
    const showSpy = mockSdkService("showNote", headRealm)
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
    expect(showSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        path: { note: notebook.headNoteId },
      })
    )
  })
})
