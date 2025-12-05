import { marked, type Tokens } from "marked"

export default function markdownToQuillHtml(
  markdown: string | undefined
): string {
  const renderer = new marked.Renderer()
  let indentLevel = -1 // Variable to track indentation level

  // Add this new helper function at the top
  const convertHtmlList = (html: string): string => {
    return html
      .replace(/<ul>/g, "<ol>")
      .replace(/<\/ul>/g, "</ol>")
      .replace(/<li>/g, '<li data-list="bullet">')
  }

  // Override the html method to handle raw HTML
  renderer.html = function (html: string | Tokens.Generic): string {
    const htmlContent = typeof html === "string" ? html : html.text
    if (htmlContent.includes("<ul>") || htmlContent.includes("<li>")) {
      return convertHtmlList(htmlContent)
    }
    return htmlContent
  }

  // Override the list method
  renderer.list = function (token: Tokens.List): string {
    indentLevel++ // Entering a list increases indent level
    // The list is always ordered for Quill
    token.ordered = true
    // Process the list items
    const body = token.items.map((item) => this.listitem(item)).join("")

    // Only wrap in <ol> when at top-level
    let result
    if (indentLevel === 0) {
      result = `<ol>${body}</ol>`
    } else {
      result = body
    }

    indentLevel-- // Exiting a list decreases indent level

    return result
  }

  // Override the listitem method
  renderer.listitem = function (token: Tokens.ListItem): string {
    // Parse the text inside the list item
    const text = this.parser!.parse(token.tokens)
    // Prepare the class attribute
    const indentClass =
      indentLevel > 0 ? ` class="ql-indent-${indentLevel}"` : ""

    const itemType = /^\d+\./.test(token.raw) ? "ordered" : "bullet"

    // Return the list item with the appropriate data-list attribute and class
    return `<li${indentClass} data-list="${itemType}">${text}</li>`
  }

  // Override the paragraph method
  renderer.paragraph = function (token: Tokens.Paragraph): string {
    const text = this.parser!.parseInline(token.tokens)
    if (indentLevel >= 0) {
      // Inside a list, don't wrap text in <p> tags
      return text
    } else {
      // Outside a list, wrap text in <p> tags
      return `<p>${text}</p>`
    }
  }

  // Override the blockquote method to remove <p> tags
  renderer.blockquote = function (token: Tokens.Blockquote): string {
    const body = this.parser!.parse(token.tokens)
    // Remove <p> and </p> tags from within blockquote
    const cleanedBody = body.replace(/<p>/g, "").replace(/<\/p>/g, "")
    return `<blockquote>${cleanedBody}</blockquote>`
  }

  // Override the code method to output <pre><code>
  renderer.code = function (token: Tokens.Code): string {
    const code = token.text || ""
    // Escape HTML entities in the code content
    const escapedCode = code
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;")
    return `<pre><code>${escapedCode}</code></pre>`
  }

  // Set up the parser with the custom renderer
  const parser = new marked.Parser({ renderer })
  renderer.parser = parser

  // Tokenize the markdown input
  const tokens = marked.lexer(markdown || "")

  // Parse the tokens into HTML
  const result = parser.parse(tokens)

  // Modify the final return to handle any remaining HTML list conversions
  return convertHtmlList(result.trim().replace(/>\s+</g, "><"))
}
