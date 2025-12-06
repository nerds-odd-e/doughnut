import { marked, type Tokens } from "marked"

export interface MarkdownToHtmlOptions {
  preserve_pre?: boolean
}

export default function markdownToQuillHtml(
  markdown: string | undefined,
  options?: MarkdownToHtmlOptions
): string {
  const preservePre = options?.preserve_pre ?? false
  const renderer = new marked.Renderer()
  let indentLevel = -1 // Variable to track indentation level

  // Add this new helper function at the top
  const convertHtmlList = (html: string): string => {
    return html
      .replace(/<ul>/g, "<ol>")
      .replace(/<\/ul>/g, "</ol>")
      .replace(/<li>/g, '<li data-list="bullet">')
  }

  // Helper function to escape HTML tags
  const escapeHtml = (html: string): string => {
    return html
      .replace(/&/g, "&amp;")
      .replace(/</g, "&lt;")
      .replace(/>/g, "&gt;")
      .replace(/"/g, "&quot;")
      .replace(/'/g, "&#39;")
  }

  // Override the html method to handle raw HTML
  renderer.html = function (html: string | Tokens.Generic): string {
    const htmlContent = typeof html === "string" ? html : html.text
    if (htmlContent.includes("<ul>") || htmlContent.includes("<li>")) {
      return convertHtmlList(htmlContent)
    }
    const trimmed = htmlContent.trim()
    // Allow <br> or <br/> (self-closing tags) and convert to <br class="softbreak">
    if (/^<br\s*\/?>$/i.test(trimmed)) {
      return '<br class="softbreak">'
    }
    // Allow complete HTML tags matching <tag xxx>...</tag> pattern
    // This matches opening tag with optional attributes, content, and closing tag
    if (/^<[^>]+>[\s\S]*<\/[^>]+>$/.test(trimmed)) {
      // Replace <br> tags inside other HTML tags with <br class="softbreak">
      return htmlContent.replace(/<br\s*\/?>/gi, '<br class="softbreak">')
    }
    // Escape all other HTML tags
    return escapeHtml(htmlContent)
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

  // Helper function to join lines with single newlines in HTML text content
  // For CJK (Chinese, Japanese, Korean) characters, join without space
  // For alphabetical text, join with space
  // For mixed text, join with space (alphabetical text needs spaces)
  const joinSingleNewlinesInHtml = (html: string): string => {
    // First, remove newlines that immediately follow HTML tag closing characters (>)
    // This handles cases like <br>\nworld where the newline should be removed
    const result = html.replace(/>\n([^\n<])/g, (_match, after) => {
      return `>${after}`
    })
    // Replace newline characters in HTML text content
    // This regex matches a newline that is between non-newline characters
    // and handles both plain text and text within HTML tags
    return result.replace(/([^\n>])\n([^\n<])/g, (_match, before, after) => {
      // Check if both sides contain CJK characters
      // CJK Unicode ranges:
      // - CJK Unified Ideographs: \u4E00-\u9FFF
      // - Hiragana: \u3040-\u309F
      // - Katakana: \u30A0-\u30FF
      // - Hangul: \uAC00-\uD7AF
      const cjkRegex = /[\u4E00-\u9FFF\u3040-\u309F\u30A0-\u30FF\uAC00-\uD7AF]/
      const beforeIsCJK = cjkRegex.test(before)
      const afterIsCJK = cjkRegex.test(after)

      // Only join without space if both sides are CJK
      // Otherwise join with space (for alphabetical or mixed text)
      return beforeIsCJK && afterIsCJK
        ? `${before}${after}`
        : `${before} ${after}`
    })
  }

  // Override the paragraph method
  renderer.paragraph = function (token: Tokens.Paragraph): string {
    // Parse inline tokens to get HTML
    let text = this.parser!.parseInline(token.tokens)

    // Join single newlines in the HTML text content
    // This handles newlines that might be preserved in the HTML output
    text = joinSingleNewlinesInHtml(text)

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

  // Override the code method to convert to Quill code block format or plain <pre>
  renderer.code = function (token: Tokens.Code): string {
    const language = token.lang || "plain"
    // Use trimEnd() to preserve leading spaces on each line
    const content = token.text.trimEnd()

    // If preserve_pre is true, use plain <pre> tags
    if (preservePre) {
      return `<pre><code>${content}</code></pre>`
    }

    // Otherwise, use Quill code block format
    // Split content by newlines to create multiple code blocks
    const lines = content.split(/\n/)
    const codeBlocks = lines.map((line) => {
      // Empty lines should be converted to <br>
      if (line.length === 0) {
        return `<div class="ql-code-block" data-language="${language}"><br></div>`
      }
      return `<div class="ql-code-block" data-language="${language}">${line}</div>`
    })
    return `<div class="ql-code-block-container" spellcheck="false">${codeBlocks.join("")}</div>`
  }

  // Helper function to wrap standalone <br> tags in <p> tags
  // Only wraps <br> tags that appear between paragraphs, not ones inside paragraphs
  const wrapStandaloneBrInParagraph = (html: string): string => {
    // Match <br class="softbreak"> that appears between </p> and <p> tags
    // This pattern matches: </p><br class="softbreak"><p>
    return html.replace(
      /<\/p><br class="softbreak"><p>/g,
      '</p><p><br class="softbreak"></p><p>'
    )
  }

  // Set up the parser with the custom renderer
  const parser = new marked.Parser({ renderer })
  renderer.parser = parser

  // Tokenize the markdown input
  const tokens = marked.lexer(markdown || "")

  // Parse the tokens into HTML
  const result = parser.parse(tokens)

  // Modify the final return to handle any remaining HTML list conversions
  // and wrap standalone <br> tags in paragraphs (only those between paragraphs)
  return wrapStandaloneBrInParagraph(
    convertHtmlList(result.trim().replace(/>\s+</g, "><"))
  )
}
