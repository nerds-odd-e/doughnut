import { screen } from "@testing-library/vue";
import Card from "@/components/notes/Card.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("A child card of a note", () => {
  it("render the card", async () => {
    const note = makeMe.aNote.title("this is a note").please();
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
  });
});
