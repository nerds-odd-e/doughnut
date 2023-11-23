
import AIClarifyingQuestionDialog from "@/components/notes/AIClarifyingQuestionDialog.vue";
import helper from "../helpers";
import { VueWrapper } from "@vue/test-utils";

describe("answering a clarifying question for note details geeration", () => {

  let wrapper: VueWrapper;

  beforeEach(() => {
    vi.useFakeTimers();
    wrapper = helper
      .component(AIClarifyingQuestionDialog)
      .withStorageProps({ question: "Do you mean American Football or European Football?" })
      .mount();
  });

  afterEach(() => {
    vi.runOnlyPendingTimers();
    vi.useRealTimers();
  });


  it("renders the questions correctly", () => {
    expect(wrapper.text()).toContain("Do you mean American Football or European Football?")
  });

  it("submitting the form propagates the submit event to the parent component", () => {
    wrapper.find('input[name="answerToAI"]').setValue('Europe Football');
    wrapper.find('form').trigger('submit');

    const [submitEvent] = wrapper.emitted().submit as string[] || [];
    expect(submitEvent?.at(0)).toEqual("Europe Football")
  });

})
