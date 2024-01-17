import AIClarifyingQuestionDialog from "@/components/notes/AIClarifyingQuestionDialog.vue";
import { VueWrapper } from "@vue/test-utils";
import helper from "../helpers";

describe("answering a clarifying question for note details geeration", () => {
  let wrapper: VueWrapper;
  const clarifyingQuestion: Generated.ClarifyingQuestion = {
    question: "Do you mean American Football or European Football?",
  };
  const clarifyingHistory = [
    {
      questionFromAI: {
        question: "Is it a sport?",
      },
      answerFromUser: "Yes",
    },
  ];

  beforeEach(() => {
    vi.useFakeTimers();
    wrapper = helper
      .component(AIClarifyingQuestionDialog)
      .withProps({ clarifyingQuestion, clarifyingHistory })
      .mount();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });

  it("renders the questions correctly", () => {
    expect(wrapper.text()).toContain(
      "Do you mean American Football or European Football?",
    );
  });

  it("renders the previous questions", () => {
    expect(wrapper.text()).toContain("Is it a sport?");
  });

  it("submitting the form propagates the submit event to the parent component", () => {
    wrapper.find('input[name="answerToAI"]').setValue("Europe Football");
    wrapper.find("form").trigger("submit");

    const [submitEvent] = (wrapper.emitted().submit as string[]) || [];
    expect(submitEvent?.at(0)).toMatchObject({
      answerFromUser: "Europe Football",
    });
  });
});
