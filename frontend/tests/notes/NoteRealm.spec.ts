import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("NoteRealm", () => {
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
        .component(NoteShowPage)
        .withStorageProps({
          noteId: noteRealm.id,
          expandChildren: true,
        })
        .render();
      await flushPromises();
      expect(screen.getAllByRole("title")).toHaveLength(1);
    });
  });
});
