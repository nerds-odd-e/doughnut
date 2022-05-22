/**
 * @jest-environment jsdom
 */
import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("comments", () => {
  let noteRealm: Generated.NoteRealm;

  beforeEach(() => {
    helper.store.featureToggle = true;
    helper.store.currentUser = makeMe.aUser().please();
    noteRealm = makeMe.aNoteRealm.please();
    const notesBulk: Generated.NoteRealmWithPosition = {
      notePosition: makeMe.aNotePosition.please(),
      noteRealm,
    };
    helper.apiMock
      .expectingGet(`/api/notes/${noteRealm.id}`)
      .andReturnOnce(notesBulk);
  });

  it("fetch comments & render", async () => {
    const comment = { content: "my comment" };
    helper.apiMock
      .expectingGet(`/api/notes/${noteRealm.id}/comments`)
      .andReturnOnce([comment]);
    helper
      .component(NoteShowPage)
      .withProps({ noteId: noteRealm.id, expandChildren: false })
      .render();
    await flushPromises();
    await screen.findByText("my comment");
  });
});
