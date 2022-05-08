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
  let noteRealm: Generated.NoteRealm;

  beforeEach(() => {
    noteRealm = makeMe.aNoteRealm.please();
    const noteRealmWithPosition: Generated.NoteRealmWithPosition = {
      notePosition: makeMe.aNotePosition.please(),
      noteRealm,
    };
    helper.apiMock
      .expectingGet(`/api/notes/${noteRealm.id}`)
      .andReturnOnce(noteRealmWithPosition);
  });

  describe("rendering a note realm", () => {
    it("should render note with one child", async () => {
      helper
        .component(NoteRealmAsync)
        .withProps({ noteId: noteRealm.id, expandChildren: true })
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
        .expectingGet(`/api/notes/${noteRealm.id}/comments`)
        .andReturnOnce([comment]);
      helper
        .component(NoteRealmAsync)
        .withProps({ noteId: noteRealm.id, expandChildren: false })
        .render();
      await flushPromises();
      await screen.findByText("my comment");
    });
  });
});
