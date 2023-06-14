export default class AiAdvisor {
  textContent: Generated.TextContent;

  constructor(textContent: Generated.TextContent) {
    this.textContent = textContent;
  }

  prompt(): string {
    return `Complete the description for the following note:\ntitle: ${
      this.textContent.title
    }\ndescription:\n---\n${this.textContent.description || ""}`;
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
