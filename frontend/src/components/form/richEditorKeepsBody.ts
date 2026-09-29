import { marked } from "marked"
import markdownizer from "./markdownizer"

const rendered = (markdown: string) =>
  (marked.parse(markdown, { async: false }) as string)
    .replace(/>\s*\n\s*</g, "><")
    .replace(/(<pre[\s\S]*?<\/pre>)|\s+/g, (_, pre) => pre ?? " ")

const blankMarkup = /^(\s|&nbsp;|<\/?p>|<br\s*\/?>)*$/i

/**
 * True when `body` renders only blank markup (empty paragraphs, line breaks,
 * non-breaking spaces, whitespace), or when the Markdown the editor would save
 * from `heldHtml` renders the same as `body`.
 */
export const richEditorKeepsBody = (body: string, heldHtml: string) => {
  const renderedBody = rendered(body)
  return (
    blankMarkup.test(renderedBody) ||
    renderedBody === rendered(markdownizer.htmlToMarkdown(heldHtml))
  )
}
