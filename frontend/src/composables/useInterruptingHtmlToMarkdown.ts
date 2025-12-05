import { marked, type Tokens } from "marked"
import markdownizer from "@/components/form/markdownizer"

export function useInterruptingHtmlToMarkdown() {
  const htmlToMarkdown = (html: string) => {
    let markdown = markdownizer.htmlToMarkdown(html)

    // Parse markdown into tokens to detect links
    const tokens = marked.lexer(markdown)
    let linkCount = 0

    // Count links using marked's walkTokens
    marked.walkTokens(tokens, (token) => {
      if (token.type === "link" || token.type === "image") {
        linkCount++
      }
    })

    if (linkCount > 2) {
      const confirmed = window.confirm(
        `Shall I remove the ${linkCount} links from the pasting content?`
      )

      if (confirmed) {
        // Remove links by converting them to text tokens
        marked.walkTokens(tokens, (token) => {
          if (token.type === "link" || token.type === "image") {
            const linkToken = token as Tokens.Link
            // Replace link/image token with text token
            Object.assign(token, {
              type: "text",
              raw: linkToken.text || "",
              text: linkToken.text || "",
            } as Tokens.Text)
          }
        })

        // Convert tokens back to markdown via HTML
        const html = marked.parser(tokens)
        markdown = markdownizer.htmlToMarkdown(html).trim()
      }
    }

    return markdown
  }

  return {
    htmlToMarkdown,
  }
}
