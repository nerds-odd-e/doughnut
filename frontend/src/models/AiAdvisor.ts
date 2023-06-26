export type AiQuestionModel = {
  question: string;
  options: AiQuestionOption[];
};

type AiQuestionOption = {
  option: string;
  correct: boolean;
};

export default class AiAdvisor {
  textContent: Generated.TextContent;

  constructor(textContent: Generated.TextContent) {
    this.textContent = textContent;
  }

  promptWithContext(): string {
    return `Please provide the description for the note titled: ${this.textContent.title}`;
  }
}
