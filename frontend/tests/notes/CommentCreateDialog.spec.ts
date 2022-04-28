/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import { flushPromises } from "@vue/test-utils";
import CommentCreateDialog from "@/components/notes/CommentCreateDialog.vue";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("create a comment", () => {
  it("renders a dialog for comment with submit button that calls API to create comment", async () => {
    const noteId = 456;
    const mockPush = jest.fn();
    helper
      .component(CommentCreateDialog)
      .withMockRouterPush(mockPush)
      .withProps({ noteId })
      .render();
    (await screen.findByText("Submit")).click();
    helper.apiMock.verifyCall(`/api/notes/${noteId}/createComment`);
    await flushPromises();
    expect(mockPush).toHaveBeenCalledWith({
      name: "noteShow",
      params: { noteId },
    });
  });
});
