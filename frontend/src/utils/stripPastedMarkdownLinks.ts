import { marked, type Tokens } from "marked"
import markdownizer from "@/components/form/markdownizer"
import { verbatimFrontmatterPrefixAndBody } from "@/utils/noteDetailsFrontmatter"

function isInternalNoteShowMarkdownHref(
  href: string | null | undefined
): boolean {
  if (!href) return false
  try {
    const u = new URL(
      href,
      href.startsWith("/") ? "http://local.invalid" : undefined
    )
    return /^\/d\/n\/\d+(\/|$)/.test(u.pathname)
  } catch {
    return false
  }
}

export function countMarkdownLinksAndImages(markdown: string): {
  linkCount: number
  imageCount: number
} {
  const tokens = marked.lexer(markdown)
  let linkCount = 0
  let imageCount = 0

  marked.walkTokens(tokens, (token) => {
    if (token.type === "link") {
      if (!isInternalNoteShowMarkdownHref((token as Tokens.Link).href)) {
        linkCount++
      }
    } else if (token.type === "image") imageCount++
  })

  return { linkCount, imageCount }
}

export function countMarkdownLinksAndImagesInNoteDetails(details: string): {
  linkCount: number
  imageCount: number
} {
  const split = verbatimFrontmatterPrefixAndBody(details)
  return countMarkdownLinksAndImages(split?.body ?? details)
}

function stripMarkdownLinksAndImages(
  markdown: string,
  removeLinks: boolean,
  removeImages: boolean
): string {
  const tokens = marked.lexer(markdown)

  marked.walkTokens(tokens, (token) => {
    if (token.type === "link" && removeLinks) {
      const linkToken = token as Tokens.Link
      const href = linkToken.href
      const asRecord = token as Record<string, unknown>
      delete asRecord.href
      delete asRecord.title
      delete asRecord.tokens
      if (isInternalNoteShowMarkdownHref(href)) {
        const label = linkToken.text || ""
        Object.assign(token, {
          type: "text",
          raw: `[[${label}]]`,
          text: `[[${label}]]`,
          escaped: false,
        } as Tokens.Text)
        return
      }
      Object.assign(token, {
        type: "text",
        raw: linkToken.text || "",
        text: linkToken.text || "",
        escaped: false,
      } as Tokens.Text)
    } else if (token.type === "image" && removeImages) {
      const imageToken = token as Tokens.Image
      const asRecord = token as Record<string, unknown>
      delete asRecord.href
      delete asRecord.title
      delete asRecord.tokens
      Object.assign(token, {
        type: "text",
        raw: imageToken.text || "",
        text: imageToken.text || "",
        escaped: false,
      } as Tokens.Text)
    }
  })

  const html = marked.parser(tokens)
  return markdownizer.htmlToMarkdown(html).trim()
}

export function stripMarkdownLinksAndImagesInNoteDetails(
  details: string,
  removeLinks: boolean,
  removeImages: boolean
): string {
  const split = verbatimFrontmatterPrefixAndBody(details)
  if (!split) {
    return stripMarkdownLinksAndImages(details, removeLinks, removeImages)
  }
  return (
    split.prefix +
    stripMarkdownLinksAndImages(split.body, removeLinks, removeImages)
  )
}
