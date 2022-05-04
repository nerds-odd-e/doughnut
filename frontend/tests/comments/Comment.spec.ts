/**
 * @jest-environment jsdom
 */
import { screen } from "@testing-library/vue";
import Comments from "@/components/notes/Comments.vue";
import flushPromises from "flush-promises";
import helper from "../helpers";

helper.resetWithApiMock(beforeEach, afterEach);

describe("comments", () => {
  const noteId = 456;

  beforeEach(() => {
    helper.store.featureToggle = true;
  });

  const queryToggleButton = () => {
    return screen.queryByRole("button", {name: "toggle comments"});
  };

  xit("should not display toggle comments button if there is no comments", async () => {
    helper.apiMock.expecting(`/api/notes/${noteId}/comments`, []);
    helper.component(Comments).withProps({ noteId }).render();
    expect(queryToggleButton()).toBeNull();
  });

  it("fetch comments & render", async () => {
    const comment = {content: "my comment"};
    helper.apiMock.expecting(`/api/notes/${noteId}/comments`, [comment]);
    helper.component(Comments).withProps({ noteId }).render();
    await queryToggleButton()?.click();
    screen.findByText("my comment");
  });
});
