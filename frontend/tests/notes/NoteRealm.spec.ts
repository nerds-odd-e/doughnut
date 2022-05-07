/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import NoteRealmAsync from "@/components/notes/NoteRealmAsync.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("comments", () => {
  let note: Generated.NoteRealm;

  beforeEach(() => {
    note = makeMe.aNoteRealm.please();
    const notesBulk: Generated.NotesBulk = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [note],
    };
    helper.apiMock
      .expectingGet(`/api/notes/${note.id}`)
      .andReturnOnce(notesBulk);
  });

  describe("rendering a note realm", () => {
    it("should render note with one child", async () => {
      helper
        .component(NoteRealmAsync)
        .withProps({ noteId: note.id, expandChildren: true })
        .render();
      await flushPromises();
      expect(screen.getAllByRole("title")).toHaveLength(1);
    });
  });

  describe("comments", () => {
    beforeEach(() => {
      helper.store.featureToggle = true;
      helper.store.currentUser = makeMe.aUser().please();
    });

    it("fetch comments & render", async () => {
      const comment = { content: "my comment" };
      helper.apiMock
        .expectingGet(`/api/notes/${note.id}/comments`)
        .andReturnOnce([comment]);
      helper
        .component(NoteRealmAsync)
        .withProps({ noteId: note.id, expandChildren: false })
        .render();
      await flushPromises();
      await screen.findByText("my comment");
    });
  });
});
