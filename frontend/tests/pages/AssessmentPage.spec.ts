import { describe, it } from "vitest";
import AssessmentPage from "@/pages/AssessmentPage.vue";
import helper from "../helpers";
import makeMe from "../fixtures/makeMe";

describe("assessment page", () => {
  beforeEach(() => {
    const teleportTarget = document.createElement("div");
    teleportTarget.id = "head-status";
    document.body.appendChild(teleportTarget);
  });

  it("calls API ONCE on mount", async () => {
    const notebook = makeMe.aNotebook.please();
    const quizQuestion = makeMe.aQuizQuestion.please();
    helper.managedApi.restAssessmentController.generateAssessmentQuestions = vi
      .fn()
      .mockResolvedValue([quizQuestion]);
    helper
      .component(AssessmentPage)
      .withProps({ notebookId: notebook.id })
      .render();
    expect(
      helper.managedApi.restAssessmentController.generateAssessmentQuestions,
    ).toBeCalledTimes(1);
  });
});
