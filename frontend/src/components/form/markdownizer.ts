import { marked } from "marked";
import TurndownService from "turndown";

export const turndownService = new TurndownService({
  blankReplacement(_, node: Node) {
    return (node as HTMLElement).outerHTML;
  },
});

export default {
  markdownToHtml(markdown: string | undefined) {
    return marked(markdown || "")
      .trim()
      .replace(/>\s+</g, "><");
  },
  htmlToMarkdown(html: string) {
    return turndownService.turndown(html);
  },
};
