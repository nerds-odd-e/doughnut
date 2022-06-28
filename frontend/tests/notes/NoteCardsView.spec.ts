/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteCardsView from "@/components/notes/views/NoteCardsView.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("note wth child cards", () => {
  it("should render note with one child", async () => {
    helper.reset();
    const noteParent = makeMe.aNoteRealm.title("parent").please();
    makeMe.aNoteRealm.title("child").under(noteParent).please();
    helper
      .component(NoteCardsView)
      .withProps({
        noteRealm: noteParent,
        expandChildren: true,
      })
      .render();
    expect(screen.getAllByRole("title")).toHaveLength(1);
    await screen.findByText("parent");
    await screen.findByText("child");
  });
});
