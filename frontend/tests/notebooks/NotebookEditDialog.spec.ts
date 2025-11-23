import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"
import NotebookEditDialog from "@/components/notebook/NotebookEditDialog.vue"
import * as sdk from "@generated/backend/sdk.gen"
import { vi } from "vitest"

describe("NoteBookEditDialog.vue", () => {
  beforeEach(() => {
    vi.spyOn(sdk, "getApprovalForNotebook").mockResolvedValue({
      data: { approval: undefined },
      error: undefined,
      request: {} as Request,
      response: {} as Response,
    })
  })

  it("shows the current number of questions in assessment if set", () => {
    const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
    const wrapper = helper
      .component(NotebookEditDialog)
      .withRouter()
      .withProps({ notebook })
      .mount()
    const input = wrapper.find(
      "input[id='notebook-numberOfQuestionsInAssessment']"
    )
    expect((input.element as HTMLInputElement).value).toBe("4")
  })
})
