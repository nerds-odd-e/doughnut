import markdownToQuillHtml, {
  type MarkdownToHtmlOptions,
} from "./markdownToQuillHtml"
import quillHtmlToMarkdown from "./quillHtmlToMarkdown"

export default {
  markdownToHtml: (
    markdown: string | undefined,
    options?: MarkdownToHtmlOptions
  ) => markdownToQuillHtml(markdown, options),
  htmlToMarkdown: quillHtmlToMarkdown,
}
