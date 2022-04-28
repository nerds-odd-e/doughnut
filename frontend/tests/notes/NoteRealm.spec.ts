/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteRealm from "@/components/notes/views/NoteRealm.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("rendering a note realm", () => {
  it("should render note with one child", async () => {
    helper.reset();
    const note = makeMe.aNoteRealm.please();
    helper.store.loadNoteRealms([note]);
    helper
      .component(NoteRealm)
      .withProps({ noteId: note.id, expandChildren: true })
      .render();
    expect(screen.getAllByRole("title")).toHaveLength(1);
  });
});
