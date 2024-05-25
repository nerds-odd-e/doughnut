import { describe, it } from "vitest";
import CircleShowPage from "@/pages/CircleShowPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("circle show page", () => {
  beforeEach(() => {
    const teleportTarget = document.createElement("div");
    teleportTarget.id = "head-status";
    document.body.appendChild(teleportTarget);
  });

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
