import makeMe from "@tests/fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import NotebookPage from "@/pages/NotebookPage.vue"
import { screen } from "@testing-library/vue"
import { flushPromises } from "@vue/test-utils"
import { beforeEach, describe, it, expect } from "vitest"

describe("NotebookPage.spec", () => {
  beforeEach(() => {
    mockSdkService("getApprovalForNotebook", { approval: undefined })
    mockSdkService("getAiAssistant", { additionalInstructionsToAi: "" })
  })

  it("shows the current number of questions in assessment if set", async () => {
    const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
    mockSdkService("get", notebook)
    helper
      .component(NotebookPage)
      .withRouter()
      .withCurrentUser(makeMe.aUser.please())
      .currentRoute({
        name: "notebookEdit",
        params: { notebookId: String(notebook.id) },
      })
      .render()
    await flushPromises()
    const input = screen.getByLabelText(/Number Of Questions In Assessment/i)
    expect((input as HTMLInputElement).value).toBe("4")
  })
})
