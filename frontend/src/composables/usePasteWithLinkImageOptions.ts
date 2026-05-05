import markdownizer from "@/components/form/markdownizer"
import usePopups from "@/components/commons/Popups/usePopups"
import {
  countMarkdownLinksAndImagesInNoteDetails,
  stripMarkdownLinksAndImagesInNoteDetails,
} from "@/utils/stripPastedMarkdownLinks"

export function usePasteWithLinkImageOptions() {
  const { popups } = usePopups()

  const htmlToMarkdown = (html: string): string =>
    markdownizer.htmlToMarkdown(html)

  const processContentAfterPaste = async (
    content: string
  ): Promise<string | null> => {
    const { linkCount, imageCount } =
      countMarkdownLinksAndImagesInNoteDetails(content)

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
      return stripMarkdownLinksAndImagesInNoteDetails(content, true, false)
    }
    if (result === "images") {
      return stripMarkdownLinksAndImagesInNoteDetails(content, false, true)
    }
    if (result === "both") {
      return stripMarkdownLinksAndImagesInNoteDetails(content, true, true)
    }

    return null
  }

  return {
    htmlToMarkdown,
    processContentAfterPaste,
  }
}
