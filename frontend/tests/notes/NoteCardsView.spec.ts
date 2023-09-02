import { screen } from "@testing-library/vue";
import NoteCardsView from "@/components/notes/views/NoteCardsView.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("note wth child cards", () => {
  it("should render note with one child", async () => {
    const noteParent = makeMe.aNoteRealm.topic("parent").please();
    makeMe.aNoteRealm.topic("child").under(noteParent).please();
    helper.apiMock
      .expectingGet(`/api/notes/${noteParent.id}`)
      .andReturnOnce(noteParent);
    helper
      .component(NoteCardsView)
      .withStorageProps({
        noteId: noteParent.id,
        expandChildren: true,
      })
      .render();
    await screen.findByText("parent");
    await screen.findByText("child");
    expect(screen.getAllByRole("topic")).toHaveLength(1);
  });
});
