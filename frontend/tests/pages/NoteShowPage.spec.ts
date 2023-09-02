import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("all in note show page", () => {
  describe("note show", () => {
    const noteRealm = makeMe.aNoteRealm.inCircle("a circle").please();

    it(" should fetch API", async () => {
      helper.apiMock
        .expectingGet(`/api/notes/${noteRealm.id}`)
        .andReturnOnce(noteRealm);
      helper
        .component(NoteShowPage)
        .withStorageProps({ noteId: noteRealm.id })
        .render();
      await screen.findByText(noteRealm.note.topic);
    });
  });
});
