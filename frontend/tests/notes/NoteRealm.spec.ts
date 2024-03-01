import { flushPromises } from "@vue/test-utils";
import { screen } from "@testing-library/vue";
import NoteShowPage from "@/pages/NoteShowPage.vue";
import { NoteRealm } from "@/generated/backend";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("NoteRealm", () => {
  let noteRealm: NoteRealm;

  beforeEach(() => {
    noteRealm = makeMe.aNoteRealm.please();
    helper.managedApi.restNoteController.show1 = vitest
      .fn()
      .mockResolvedValue(noteRealm);
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
      expect(screen.getAllByRole("topic")).toHaveLength(1);
      expect(helper.managedApi.restNoteController.show1).toBeCalledWith(
        noteRealm.id,
      );
    });
  });
});
