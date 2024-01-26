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
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
    await screen.findByText("the details");
  });

  it("truncate the details", async () => {
    const note = makeMe.aNote
      .topicConstructor("this is a note")
      .details("nah ".repeat(20))
      .please();
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
    await screen.findByText(
      "nah nah nah nah nah nah nah nah nah nah nah nah...",
    );
  });

  it("removes the html tags", async () => {
    const note = makeMe.aNote
      .topicConstructor("this is a note")
      .details("<p>nah</p>".repeat(20))
      .please();
    helper.component(Card).withStorageProps({ note }).render();
    await screen.findByText("this is a note");
    await screen.findByText(
      "nahnahnahnahnahnahnahnahnahnahnahnahnahnahnahna...",
    );
  });
});
