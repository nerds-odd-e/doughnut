/**
 * @jest-environment jsdom
 */
import NoteEditDialog from "@/components/notes/NoteEditDialog.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("note show", () => {
  it("fetch API to be called ONCE", async () => {
    const noteRealm = makeMe.aNoteRealm.please();
    const stubResponse = {
      notePosition: makeMe.aNotePosition.please(),
      notes: [noteRealm],
    };

    helper.apiMock
      .expectingGet(`/api/notes/${noteRealm.id}`)
      .andReturnOnce(stubResponse);
    helper
      .component(NoteEditDialog)
      .withProps({ noteId: noteRealm.id })
      .render();
  });
});
