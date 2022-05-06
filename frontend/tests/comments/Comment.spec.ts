/**
 * @jest-environment jsdom
 */
import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import NoteRealmAsync from "@/components/notes/NoteRealmAsync.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("comments", () => {
  let note: Generated.NoteRealm;

  beforeEach(() => {
    helper.store.featureToggle = true;
    helper.store.currentUser = makeMe.aUser().please();
    note = makeMe.aNoteRealm.please();
    const notesBulk: Generated.NotesBulk = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [note],
    };
    helper.apiMock
      .expectingGet(`/api/notes/${note.id}`)
      .andReturnOnce(notesBulk);
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
