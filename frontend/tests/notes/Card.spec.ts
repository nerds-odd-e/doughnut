import { screen } from "@testing-library/vue";
import Card from "@/components/notes/Card.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("A child card of a note", () => {
  it("render the card", async () => {
    const note = makeMe.aNote
      .title("this is a note")
      .description("the description")
      .please();
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
    await screen.findByText("the description");
  });

  it("truncate the description", async () => {
    const note = makeMe.aNote
      .title("this is a note")
      .description("nah ".repeat(20))
      .please();
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
    await screen.findByText(
      "nah nah nah nah nah nah nah nah nah nah nah nah..."
    );
  });

  it("removes the html tags", async () => {
    const note = makeMe.aNote
      .title("this is a note")
      .description("<p>nah</p>".repeat(20))
      .please();
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
    await screen.findByText(
      "nahnahnahnahnahnahnahnahnahnahnahnahnahnahnahna..."
    );
  });
});
