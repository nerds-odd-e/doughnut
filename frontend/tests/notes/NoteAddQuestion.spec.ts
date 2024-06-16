import { screen } from "@testing-library/vue";
import userEvent from "@testing-library/user-event";
import { flushPromises } from "@vue/test-utils";
import { beforeEach } from "vitest";
import NoteAddQuestion from "@/components/notes/NoteAddQuestion.vue";
import makeMe from "../fixtures/makeMe";
import helper from "../helpers";

const note = makeMe.aNoteRealm.please();
const createWrapper = async () => {
  helper
    .component(NoteAddQuestion)
    .withProps({
      note: note.note,
    })
    .render();
  await flushPromises();
};

describe("NoteAddQuestion", () => {
  const mockedGenerateQuestion = vitest.fn();

  beforeEach(() => {
    helper.managedApi.restQuizQuestionController.generateQuestion =
      mockedGenerateQuestion;
  });

  interface Case {
    question: Record<string, string>;
    expectedRefineButton: boolean;
    expectedGenerateButton: boolean;
  }
  [
    {
      question: { Stem: "abc" },
      expectedRefineButton: false,
      expectedGenerateButton: true,
    },
  ].forEach(async (testCase: Case) => {
    it("only allow generation when no changes", async () => {
      await createWrapper();
      Object.keys(testCase.question).forEach(async (key) => {
        const ctrl = await screen.findByLabelText(key);
        await userEvent.type(ctrl, testCase.question[key]!);
        // Check the state of the buttons after typing
        const refineButton = screen.getByRole<HTMLInputElement>("button", {
          name: /refine/i,
        });
        const generateButton = screen.getByRole<HTMLInputElement>("button", {
          name: /generate/i,
        });

        expect(refineButton.disabled).toBe(testCase.expectedGenerateButton);
        expect(generateButton.disabled).toBe(testCase.expectedGenerateButton);
      });
    });
  });
});
