import { marked, type Tokens } from "marked"
import markdownizer from "@/components/form/markdownizer"
import usePopups from "@/components/commons/Popups/usePopups"

function countLinksAndImages(markdown: string) {
  const tokens = marked.lexer(markdown)
  let linkCount = 0
  let imageCount = 0

  marked.walkTokens(tokens, (token) => {
    if (token.type === "link") linkCount++
    else if (token.type === "image") imageCount++
  })

  return { linkCount, imageCount }
}

function removeLinksAndImages(
  markdown: string,
  removeLinks: boolean,
  removeImages: boolean
): string {
  const tokens = marked.lexer(markdown)

  marked.walkTokens(tokens, (token) => {
    if (token.type === "link" && removeLinks) {
      const linkToken = token as Tokens.Link
      Object.assign(token, {
        type: "text",
        raw: linkToken.text || "",
        text: linkToken.text || "",
      } as Tokens.Text)
    } else if (token.type === "image" && removeImages) {
      const imageToken = token as Tokens.Image
      Object.assign(token, {
        type: "text",
        raw: imageToken.text || "",
        text: imageToken.text || "",
      } as Tokens.Text)
    }
  })

  const html = marked.parser(tokens)
  return markdownizer.htmlToMarkdown(html).trim()
}

export function usePasteWithLinkImageOptions() {
  const { popups } = usePopups()

  const htmlToMarkdown = (html: string): string => {
    return markdownizer.htmlToMarkdown(html)
  }

  const processContentAfterPaste = async (
    content: string
  ): Promise<string | null> => {
    const { linkCount, imageCount } = countLinksAndImages(content)

    if (linkCount === 0 && imageCount === 0) {
      return null
    }

    const options: { label: string; value: string }[] = []
    const hasLinks = linkCount > 0
    const hasImages = imageCount > 0

    if (hasLinks) {
      options.push({ label: `Remove ${linkCount} links`, value: "links" })
    }
    if (hasImages) {
      options.push({ label: `Remove ${imageCount} images`, value: "images" })
    }
    if (hasLinks && hasImages) {
      options.push({ label: "Remove both", value: "both" })
    }

    const message =
      hasLinks && hasImages
        ? `The content contains ${linkCount} links and ${imageCount} images.`
        : hasLinks
          ? `The content contains ${linkCount} links.`
          : `The content contains ${imageCount} images.`

    const result = await popups.options(message, options)

    if (result === "links") {
      return removeLinksAndImages(content, true, false)
    }
    if (result === "images") {
      return removeLinksAndImages(content, false, true)
    }
    if (result === "both") {
      return removeLinksAndImages(content, true, true)
    }

    return null
  }

  return {
    htmlToMarkdown,
    processContentAfterPaste,
  }
}
