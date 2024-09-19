import { marked, type Tokens } from "marked"

export default function markdownToQuillHtml(
  markdown: string | undefined
): string {
  const renderer = new marked.Renderer()
  let indentLevel = -1 // Variable to track indentation level

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

  // Set up the parser with the custom renderer
  const parser = new marked.Parser({ renderer })
  renderer.parser = parser

  // Tokenize the markdown input
  const tokens = marked.lexer(markdown || "")

  // Parse the tokens into HTML
  const result = parser.parse(tokens)

  // Clean up the HTML output
  return result.trim().replace(/>\s+</g, "><")
}
