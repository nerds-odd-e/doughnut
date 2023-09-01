import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("NoteRealm", () => {
  let noteRealm: Generated.NoteRealm;

  beforeEach(() => {
    noteRealm = makeMe.aNoteRealm.please();
    helper.apiMock
      .expectingGet(`/api/notes/${noteRealm.id}`)
      .andReturnOnce(noteRealm);
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
