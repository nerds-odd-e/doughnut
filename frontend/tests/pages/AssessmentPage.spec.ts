import { screen } from "@testing-library/vue";
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

  describe("assessment with one quesiton", () => {
    const notebook = makeMe.aNotebook.please();
    const quizQuestion = makeMe.aQuizQuestion.please();
    beforeEach(() => {
      helper.managedApi.restAssessmentController.generateAssessmentQuestions =
        vi.fn().mockResolvedValue([quizQuestion]);
    });

    it("calls API ONCE on mount", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render();
      expect(
        helper.managedApi.restAssessmentController.generateAssessmentQuestions,
      ).toBeCalledTimes(1);
    });

    it("renders the questions", async () => {
      helper
        .component(AssessmentPage)
        .withProps({ notebookId: notebook.id })
        .render();
      await screen.findByText(quizQuestion.multipleChoicesQuestion.stem!);
    });
  });
});
