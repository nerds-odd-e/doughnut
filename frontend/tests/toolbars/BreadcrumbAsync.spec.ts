import BreadcrumbAsync from "@/components/toolbars/BreadcrumbAsync.vue";
import { flushPromises } from "@vue/test-utils";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const notePosition = makeMe.aNotePosition.please();
    const noteId = 123;

    beforeEach(async () => {
      helper.apiMock
        .expectingGet(`/api/notes/${noteId}/position`)
        .andReturnOnce(notePosition);
    });

    it("render the breadcrumber", async () => {
      const wrapper = helper
        .component(BreadcrumbAsync)
        .withProps({ noteId })
        .mount();
      await flushPromises();
      expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
    });

    it("reload when id changes", async () => {
      const wrapper = helper
        .component(BreadcrumbAsync)
        .withProps({ noteId })
        .mount();
      await flushPromises();
      const anotherNotePosition = makeMe.aNotePosition.inBazaar().please();
      const anotherNoteId = noteId + 1;
      helper.apiMock
        .expectingGet(`/api/notes/${anotherNoteId}/position`)
        .andReturnOnce(anotherNotePosition);
      wrapper.setProps({ noteId: anotherNoteId });
      await flushPromises();
      expect(wrapper.find(".breadcrumb-item").text()).toEqual("Bazaar");
    });
  });
});
