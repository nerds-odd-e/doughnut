import { screen } from "@testing-library/vue";
import NoteCardsView from "@/components/notes/views/NoteCardsView.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("note wth child cards", () => {
  it("should render note with one child", async () => {
    const noteParent = makeMe.aNoteRealm.topicConstructor("parent").please();
    makeMe.aNoteRealm.topicConstructor("child").under(noteParent).please();
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(noteParent);
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
    expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
      noteParent.id,
    );
  });
});
