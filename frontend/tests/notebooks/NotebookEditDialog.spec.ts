import makeMe from "tests/fixtures/makeMe"
import helper from "../helpers"
import NotebookEditDialog from "../../src/components/notebook/NotebookEditDialog.vue"

describe("NoteBookEditDialog.vue", () => {
  it("shows the current number of questions in assessment if set", () => {
    const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
    const wrapper = helper
      .component(NotebookEditDialog)
      .withProps({ notebook })
      .mount()
    const input = wrapper.find(
      "input[id='notebook-numberOfQuestionsInAssessment']"
    )
    expect((input.element as HTMLInputElement).value).toBe("4")
  })

  it("shows the name of the one who can certifies if set", () => {
    const notebook = makeMe.aNotebook.certifiedBy("Some Name").please()
    const wrapper = helper
      .component(NotebookEditDialog)
      .withProps({ notebook })
      .mount()
    const input = wrapper.find("input[id='notebook-certifiedBy']")
    expect((input.element as HTMLInputElement).value).toBe("Some Name")
  })
})
