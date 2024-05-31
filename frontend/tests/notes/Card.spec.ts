import { screen } from "@testing-library/vue";
import Card from "@/components/notes/Card.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("A child card of a note", () => {
  it("render the card", async () => {
    const note = makeMe.aNote
      .topicConstructor("this is a note")
      .details("the details")
      .please();
    helper.component(Card).withProps({ noteTopic: note.noteTopic }).render();
    await screen.findByText("this is a note");
    await screen.findByText("the details, just shorter");
  });
});
