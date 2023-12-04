import { marked } from "marked";
import TurndownService from "turndown";

export const turndownService = new TurndownService({
  br: "<br>",
});

turndownService.addRule("p", {
  filter: "p",
  replacement(_, node: Node) {
    const replacement = (node as HTMLElement).innerHTML;
    if (replacement === "<br>") {
      return (node as HTMLElement).outerHTML;
    }
    return replacement
      ? `\n\n${turndownService.turndown(replacement)}\n\n`
      : "";
  },
});

export default {
  markdownToHtml(markdown: string | undefined): string {
    const result = marked(markdown || "") as string;
    return result.trim().replace(/>\s+</g, "><");
  },
  htmlToMarkdown(html: string) {
    return turndownService.turndown(html);
  },
};
