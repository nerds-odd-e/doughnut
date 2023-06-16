export type AiQuestionType = {
  question: string;
  options: AiQuestionOptionType[];
};

export type AiQuestionOptionType = {
  option: string;
  correct: boolean;
  explanation: string;
};

const questionResponseFormatExample = {
  question: "",
  options: [
    {
      option: "",
      correct: true,
      explanation: "",
    },
  ],
};
export default class AiAdvisor {
  textContent: Generated.TextContent;

  constructor(textContent: Generated.TextContent) {
    this.textContent = textContent;
  }

  promptWithContext(context: string): string {
    return `Complete the description for the following note:\ncontext: ${
      context || ""
    }\ntitle: ${this.textContent.title}\ndescription:\n---\n${
      this.textContent.description || ""
    }`;
  }

  questionPrompt(): string {
    return `Given the following text fragments: [ "${
      this.textContent.title
    }", "${
      this.textContent.description
    }" ], generate a multiple-choice question with exactly 3 options and exactly 1 correct option. The response should be JSON-formatted as follows: ${JSON.stringify(
      questionResponseFormatExample
    )}`;
  }

  // eslint-disable-next-line class-methods-use-this
  processResult(suggestion: string): string {
    const separator = "---\n";
    const parts = suggestion.split(separator);

    if (parts.length > 1) {
      parts.shift();
      return parts.join(separator);
    }

    return suggestion;
  }
}
