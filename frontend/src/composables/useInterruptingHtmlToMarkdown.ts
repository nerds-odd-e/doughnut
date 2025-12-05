import { marked, type Tokens } from "marked"
import markdownizer from "@/components/form/markdownizer"

type MarkdownToken = ReturnType<typeof marked.lexer>[number]

function findLinkTokens(tokens: MarkdownToken[]): Tokens.Link[] {
  const links: Tokens.Link[] = []

  for (const token of tokens) {
    if (token.type === "link" || token.type === "image") {
      links.push(token as Tokens.Link)
    }

    // Recursively search in nested tokens
    if ("tokens" in token && token.tokens) {
      links.push(...findLinkTokens(token.tokens))
    }

    // Search in list items
    if (token.type === "list") {
      for (const item of token.items) {
        if (item.tokens) {
          links.push(...findLinkTokens(item.tokens))
        }
      }
    }
  }

  return links
}

function removeLinksFromTokens(tokens: MarkdownToken[]): MarkdownToken[] {
  return tokens.flatMap((token) => {
    // Replace link and image tokens with their text content
    if (token.type === "link") {
      // Replace link with its text tokens or a text token with the link text
      if (token.tokens && token.tokens.length > 0) {
        return removeLinksFromTokens(token.tokens)
      }
      return [
        {
          type: "text",
          raw: token.text,
          text: token.text,
        } as Tokens.Text,
      ]
    }

    if (token.type === "image") {
      // For images, replace with alt text or empty
      return [
        {
          type: "text",
          raw: token.text || "",
          text: token.text || "",
        } as Tokens.Text,
      ]
    }

    // Process nested tokens
    if ("tokens" in token && token.tokens) {
      const processedToken = { ...token }
      processedToken.tokens = removeLinksFromTokens(token.tokens)
      return [processedToken]
    }

    // Process list items
    if (token.type === "list") {
      const processedToken = { ...token } as Tokens.List
      processedToken.items = token.items.map((item) => {
        const processedItem = { ...item }
        if (item.tokens) {
          processedItem.tokens = removeLinksFromTokens(item.tokens)
        }
        return processedItem
      })
      return [processedToken]
    }

    return [token]
  })
}

function tokensToMarkdown(tokens: MarkdownToken[]): string {
  return tokens
    .map((token) => {
      switch (token.type) {
        case "paragraph": {
          const text = token.tokens ? tokensToMarkdown(token.tokens) : token.raw
          return text ? `${text}\n\n` : ""
        }
        case "heading": {
          const prefix = "#".repeat(token.depth)
          const text = token.tokens
            ? tokensToMarkdown(token.tokens)
            : token.text
          return `${prefix} ${text}\n\n`
        }
        case "list": {
          const items = token.items
            .map((item) => {
              const marker = token.ordered ? "1." : "-"
              const text = item.tokens
                ? tokensToMarkdown(item.tokens)
                : item.text || ""
              return `${marker} ${text}`
            })
            .join("\n")
          return `${items}\n\n`
        }
        case "code": {
          return `\`\`\`${token.lang || ""}\n${token.text}\n\`\`\`\n\n`
        }
        case "blockquote": {
          const text = token.tokens
            ? tokensToMarkdown(token.tokens)
            : token.text
          return `${text
            .split("\n")
            .map((line) => `> ${line}`)
            .join("\n")}\n\n`
        }
        case "text": {
          // For text tokens, process inline tokens if present
          if (token.tokens && token.tokens.length > 0) {
            return tokensToMarkdown(token.tokens)
          }
          return token.text
        }
        case "strong": {
          const text = token.tokens
            ? tokensToMarkdown(token.tokens)
            : token.text
          return `**${text}**`
        }
        case "em": {
          const text = token.tokens
            ? tokensToMarkdown(token.tokens)
            : token.text
          return `*${text}*`
        }
        case "codespan": {
          return `\`${token.text}\``
        }
        case "br": {
          return "\n"
        }
        case "hr": {
          return "---\n\n"
        }
        case "space": {
          return token.raw
        }
        case "link":
        case "image": {
          // These should be filtered out, but handle just in case
          return token.type === "link" ? token.text : token.text || ""
        }
        default: {
          // For any other token types, try to use raw or text
          return "raw" in token ? token.raw : ""
        }
      }
    })
    .join("")
}

export function useInterruptingHtmlToMarkdown() {
  const htmlToMarkdown = (html: string) => {
    let markdown = markdownizer.htmlToMarkdown(html)

    // Parse markdown into tokens to detect links in a structured way
    const tokens = marked.lexer(markdown)
    const linkTokens = findLinkTokens(tokens)
    const linkCount = linkTokens.length

    if (linkCount > 2) {
      const confirmed = window.confirm(
        `Shall I remove the ${linkCount} links from the pasting content?`
      )

      if (confirmed) {
        // Remove links from tokens and reconstruct markdown
        const tokensWithoutLinks = removeLinksFromTokens(tokens)
        markdown = tokensToMarkdown(tokensWithoutLinks).trim()
      }
    }

    return markdown
  }

  return {
    htmlToMarkdown,
  }
}
