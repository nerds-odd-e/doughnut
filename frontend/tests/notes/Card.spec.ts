import Card from "@/components/notes/Card.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("Card", () => {
  it("renders the card with title and details", async () => {
    const note = makeMe.aNote
      .topicConstructor("this is a note")
      .details("the details")
      .please()
    helper
      .component(Card)
      .withProps({ noteTopology: note.noteTopology })
      .render()
    await screen.findByText("this is a note")
    await screen.findByText("the details, just shorter")
  })

  it("does not add border when notebookId is not provided", async () => {
    const note = makeMe.aNote.topicConstructor("this is a note").please()

    const wrapper = helper
      .component(Card)
      .withProps({ noteTopology: note.noteTopology })
      .mount()

    const card = wrapper.find('[role="card"]')
    expect(card.classes()).not.toContain("different-notebook-border")
  })

  it("does not add border when notebookId matches", async () => {
    const note = makeMe.aNote.topicConstructor("this is a note").please()

    const noteTopologyWithNotebookId = {
      ...note.noteTopology,
      notebookId: 10,
    }

    const wrapper = helper
      .component(Card)
      .withProps({ noteTopology: noteTopologyWithNotebookId, notebookId: 10 })
      .mount()

    const card = wrapper.find('[role="card"]')
    expect(card.classes()).not.toContain("different-notebook-border")
  })

  it("adds border when notebookId does not match", async () => {
    const note = makeMe.aNote.topicConstructor("this is a note").please()

    const noteTopologyWithNotebookId = {
      ...note.noteTopology,
      notebookId: 10,
    }

    const wrapper = helper
      .component(Card)
      .withProps({ noteTopology: noteTopologyWithNotebookId, notebookId: 20 })
      .mount()

    const card = wrapper.find('[role="card"]')
    expect(card.classes()).toContain("different-notebook-border")
  })
})
