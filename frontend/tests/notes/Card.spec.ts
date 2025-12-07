import Card from "@/components/notes/Card.vue"
import { screen } from "@testing-library/vue"
import makeMe from "@tests/fixtures/makeMe"
import helper from "@tests/helpers"

describe("Card", () => {
  it("renders the card with title and details", async () => {
    const note = makeMe.aNote
      .titleConstructor("this is a note")
      .details("the details")
      .please()
    helper
      .component(Card)
      .withProps({ noteTopology: note.noteTopology })
      .render()
    await screen.findByText("this is a note")
    await screen.findByText("the details, just shorter")
  })
})
