import { marked } from "marked"
import Quill from "quill"
import markdownizer from "./markdownizer"
import { donutQuillOptions } from "./donutQuillOptions"
import { registerDonutQuillBlots } from "./registerDonutQuillBlots"

const parsed = (markdown: string) =>
  marked.parse(markdown, { async: false }) as string

const rendered = (markdown: string) =>
  parsed(markdown)
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

const inlineTags = new Set([
  "A",
  "B",
  "CODE",
  "DEL",
  "EM",
  "I",
  "IMG",
  "INPUT",
  "MARK",
  "S",
  "SPAN",
  "STRONG",
  "SUB",
  "SUP",
  "U",
])

/** The words of `markdown` as rendered, one entry per block or line. */
const readableLines = (markdown: string) => {
  const root = document.createElement("div")
  root.innerHTML = parsed(markdown)
  let text = ""
  const walk = (node: Node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      text += node.textContent
      return
    }
    const block = !inlineTags.has(node.nodeName)
    if (block) text += "\n"
    node.childNodes.forEach(walk)
    if (block) text += "\n"
  }
  walk(root)
  return text
    .split("\n")
    .map((line) => line.replace(/\s+/g, " ").trim())
    .filter(Boolean)
}

const sameReadableText = (a: string, b: string) =>
  readableLines(a).join("\n") === readableLines(b).join("\n")

const plainParagraphs = (lines: string[]) => {
  const root = document.createElement("div")
  for (const line of lines) {
    const p = document.createElement("p")
    p.textContent = line
    root.appendChild(p)
  }
  return markdownizer.htmlToMarkdown(root.innerHTML)
}

/**
 * A body the rich editor keeps, with the readable text of `body`: each
 * top-level block stays when the editor keeps it, becomes the editor's
 * Markdown for it when that keeps its readable text, and otherwise becomes
 * its readable lines as plain paragraphs. The joined blocks are then treated
 * the same way as a whole, since the editor may merge adjacent blocks.
 */
export const bodyTheRichEditorKeeps = (body: string) => {
  registerDonutQuillBlots()
  const quill = new Quill(
    document.createElement("div"),
    donutQuillOptions(true, "")
  )
  const heldHtml = (block: string) => {
    quill.root.innerHTML = markdownizer.markdownToHtml(block)
    quill.update(Quill.sources.SILENT)
    return quill.root.innerHTML
  }
  const asTheEditorKeeps = (markdown: string) => {
    const held = heldHtml(markdown)
    if (richEditorKeepsBody(markdown, held)) return markdown
    const heldMarkdown = markdownizer.htmlToMarkdown(held)
    if (
      sameReadableText(markdown, heldMarkdown) &&
      richEditorKeepsBody(heldMarkdown, heldHtml(heldMarkdown))
    ) {
      return heldMarkdown
    }
    return plainParagraphs(readableLines(markdown))
  }
  return asTheEditorKeeps(
    marked
      .lexer(body)
      .filter((token) => token.type !== "space")
      .map(({ raw }) => asTheEditorKeeps(raw.replace(/\n+$/, "")))
      .join("\n\n")
  )
}
