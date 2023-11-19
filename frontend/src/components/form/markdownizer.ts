import { marked } from "marked";
import TurndownService from "turndown";

export const turndownService = new TurndownService();

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
