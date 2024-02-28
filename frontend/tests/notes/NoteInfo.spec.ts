import { flushPromises } from "@vue/test-utils";
import NoteInfoButton from "@/components/notes/NoteInfoButton.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

const stubResponse = {
  reviewPoint: makeMe.aReviewPoint.please(),
  note: makeMe.aNoteRealm.please(),
};

describe("note info", () => {
  it("should render values", async () => {
    helper.apiMock
      .expectingGet(`/api/notes/123/note-info`)
      .andReturnOnce(stubResponse);
    const wrapper = helper
      .component(NoteInfoButton)
      .withProps({
        noteId: 123,
        expanded: true,
      })
      .mount();
    await flushPromises();
    expect(wrapper.findAll(".statistics-value")).toHaveLength(5);
  });
});
