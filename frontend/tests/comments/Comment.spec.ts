/**
 * @jest-environment jsdom
 */
import Comments from "@/components/notes/Comments.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("comments", () => {
  it("fetch API to be called ONCE", async () => {
    const noteId = 456;
    helper.component(Comments).withProps({ noteId }).mount();
    await flushPromises();
    helper.apiMock.verifyCall(`/api/notes/${noteId}/comments`);
  });
});
