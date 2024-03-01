import { describe, it, beforeEach, afterEach } from "vitest";
import CircleShowPage from "@/pages/CircleShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

helper.resetWithApiMock(beforeEach, afterEach);

describe("circle show page", () => {
  it("fetch API to be called ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please();
    const circleNote = makeMe.aCircleNote.notebooks(notebook).please();
    helper.managedApi.restCircleController.showCircle = vi
      .fn()
      .mockResolvedValue(circleNote);
    helper
      .component(CircleShowPage)
      .withStorageProps({ circleId: circleNote.id })
      .render();
    expect(helper.managedApi.restCircleController.showCircle).toBeCalledWith(
      circleNote.id,
    );
  });
});
