import TurndownService from "turndown"
import markdownToQuillHtml from "./markdownToQuillHtml"
import quillHtmlToMarkdown from "./quillHtmlToMarkdown"

export const turndownService = new TurndownService({
  br: "<br>",
})

turndownService.addRule("p", {
  filter: "p",
  replacement(_, node: Node) {
    const replacement = (node as HTMLElement).innerHTML
    return replacement ? `\n\n${turndownService.turndown(replacement)}\n\n` : ""
  },
})

export default {
  markdownToHtml: (
    markdown: string | undefined,
    options?: { preserve_pre?: boolean }
  ) => markdownToQuillHtml(markdown, options),
  htmlToMarkdown: quillHtmlToMarkdown,
}
