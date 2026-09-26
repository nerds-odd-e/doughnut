import { marked } from "marked"
import markdownizer from "./markdownizer"

const rendered = (markdown: string) =>
  (marked.parse(markdown, { async: false }) as string).replace(
    />\s*\n\s*</g,
    "><"
  )

/** True when the Markdown the editor would save from `heldHtml` renders the same as `body`. */
export const richEditorKeepsBody = (body: string, heldHtml: string) =>
  rendered(body) === rendered(markdownizer.htmlToMarkdown(heldHtml))
