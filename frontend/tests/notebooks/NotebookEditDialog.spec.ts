import makeMe from 'tests/fixtures/makeMe'
import helper from "../helpers"
import NotebookEditDialog from "../../src/components/notebook/NotebookEditDialog.vue"

describe('NoteBookEditDialog.vue', () => {

      it('shows the current number of questions in assessment if set', () => {
        const notebook = makeMe.aNotebook.numberOfQuestionsInAssessment(4).please()
        const wrapper = helper.component(NotebookEditDialog).withProps({notebook}).mount()
        const input = wrapper.find("input[id='notebook-numberOfQuestionsInAssessment']")
        expect((input.element as HTMLInputElement).value).toBe('4')
      })
})
