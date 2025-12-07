import { marked, type Tokens } from "marked"
import markdownizer from "@/components/form/markdownizer"

export function useInterruptingHtmlToMarkdown() {
  const htmlToMarkdown = (html: string) => {
    let markdown = markdownizer.htmlToMarkdown(html)

    // Parse markdown into tokens to detect links and images
    const tokens = marked.lexer(markdown)
    let linkCount = 0
    let imageCount = 0

    // Count links and images separately using marked's walkTokens
    marked.walkTokens(tokens, (token) => {
      if (token.type === "link") {
        linkCount++
      } else if (token.type === "image") {
        imageCount++
      }
    })

    let shouldRemoveLinks = false
    let shouldRemoveImages = false

    // Ask for confirmation separately for links
    if (linkCount > 0) {
      shouldRemoveLinks = window.confirm(
        `Shall I remove the ${linkCount} links from the pasting content?`
      )
    }

    // Ask for confirmation separately for images
    if (imageCount > 0) {
      shouldRemoveImages = window.confirm(
        `Shall I remove the ${imageCount} images from the pasting content?`
      )
    }

    // Remove links and/or images if confirmed
    if (shouldRemoveLinks || shouldRemoveImages) {
      marked.walkTokens(tokens, (token) => {
        if (token.type === "link" && shouldRemoveLinks) {
          const linkToken = token as Tokens.Link
          // Replace link token with text token
          Object.assign(token, {
            type: "text",
            raw: linkToken.text || "",
            text: linkToken.text || "",
          } as Tokens.Text)
        } else if (token.type === "image" && shouldRemoveImages) {
          const imageToken = token as Tokens.Image
          // Replace image token with text token (using alt text)
          Object.assign(token, {
            type: "text",
            raw: imageToken.text || "",
            text: imageToken.text || "",
          } as Tokens.Text)
        }
      })

      // Convert tokens back to markdown via HTML
      const html = marked.parser(tokens)
      markdown = markdownizer.htmlToMarkdown(html).trim()
    }

    return markdown
  }

  return {
    htmlToMarkdown,
  }
}
