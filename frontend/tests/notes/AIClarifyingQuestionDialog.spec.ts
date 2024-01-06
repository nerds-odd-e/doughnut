import AIClarifyingQuestionDialog from "@/components/notes/AIClarifyingQuestionDialog.vue";
import { VueWrapper } from "@vue/test-utils";
import helper from "../helpers";

describe("answering a clarifying question for note details geeration", () => {
  let wrapper: VueWrapper;
  const completionInProgress: Generated.AiCompletionResponse = {
    threadId: "123",
    moreCompleteContent: "Football",
    clarifyingQuestionRequiredAction: {
      toolCallId: "123",
      clarifyingQuestion: {
        question: "Do you mean American Football or European Football?",
      },
    },
    finishReason: "clarifying_question",
  };

  beforeEach(() => {
    vi.useFakeTimers();
    wrapper = helper
      .component(AIClarifyingQuestionDialog)
      .withProps({ completionInProgress })
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

  it("submitting the form propagates the submit event to the parent component", () => {
    wrapper.find('input[name="answerToAI"]').setValue("Europe Football");
    wrapper.find("form").trigger("submit");

    const [submitEvent] = (wrapper.emitted().submit as string[]) || [];
    expect(submitEvent?.at(0)).toMatchObject({
      answerFromUser: "Europe Football",
    });
  });
});
