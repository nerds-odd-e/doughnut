import { flushPromises } from "@vue/test-utils";
import BreadcrumbAsync from "@/components/toolbars/BreadcrumbAsync.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("repetition page", () => {
  describe("repetition page for a link", () => {
    const notePosition = makeMe.aNotePosition.please();
    const noteId = 123;
    const mockedNotePositionCall = vitest
      .fn()
      .mockResolvedValueOnce(notePosition);

    beforeEach(async () => {
      helper.managedApi.restNoteController.getPosition = mockedNotePositionCall;
    });

    it("render the breadcrumber", async () => {
      const wrapper = helper
        .component(BreadcrumbAsync)
        .withProps({ noteId })
        .mount();
      await flushPromises();
      expect(wrapper.find(".breadcrumb-item").text()).toEqual("My Notes");
      expect(mockedNotePositionCall).toHaveBeenCalledWith(noteId);
    });

    it("reload when id changes", async () => {
      const wrapper = helper
        .component(BreadcrumbAsync)
        .withProps({ noteId })
        .mount();
      await flushPromises();
      const anotherNotePosition = makeMe.aNotePosition.inBazaar().please();
      const anotherNoteId = noteId + 1;
      mockedNotePositionCall.mockResolvedValueOnce(anotherNotePosition);
      wrapper.setProps({ noteId: anotherNoteId });
      await flushPromises();
      expect(wrapper.find(".breadcrumb-item").text()).toEqual("Bazaar");
      expect(mockedNotePositionCall).toHaveBeenCalledWith(anotherNoteId);
    });
  });
});
