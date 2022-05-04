/**
 * @jest-environment jsdom
 */
import Comments from "@/components/notes/Comments.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("comments", () => {
  const noteId = 456;

  beforeEach(() => {
    helper.store.featureToggle = true;
  });

  it("should not call api before clicked", async () => {
    const wrapper = helper.component(Comments).withProps({ noteId }).mount();
    await flushPromises();
    expect(wrapper.findAll(".comment").length).toBe(0);
    // no api call should happen, this is checked in the afterEach
  });

  it("fetch comments & render", async () => {
    const comment = {};
    const wrapper = helper.component(Comments).withProps({ noteId }).mount();
    helper.apiMock.expecting(`/api/notes/${noteId}/comments`, [comment]);
    await wrapper.find(".comments").trigger("click");
    await flushPromises();
    expect(wrapper.findAll(".comment").length).toBe(1);
  });
});
