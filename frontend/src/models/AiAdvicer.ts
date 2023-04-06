export default class AiAdvicer {
  textContent: Generated.TextContent;

  constructor(textContent: Generated.TextContent) {
    this.textContent = textContent;
  }

  prompt(): string {
    return (
      this.textContent.description?.replace(/<\/?[^>]+(>|$)/g, "").trim() ||
      this.suggestion()
    );
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

  private suggestion(): string {
    return `Complete the description for the following note:\ntitle: ${this.textContent.title}\ndescription:\n---\n`;
  }
}
