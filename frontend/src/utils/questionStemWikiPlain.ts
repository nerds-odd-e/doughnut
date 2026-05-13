import {
  isValidPropertyWikiInner,
  splitWikiLinkInner,
} from "@/utils/wikiPropertyValueField"

const WELL_FORMED_WIKI_SEGMENT = /\[\[([^\[\]\r\n]*)\]\]/g

export function replaceWellFormedWikiLinksWithDisplayPlain(
  markdown: string
): string {
  return markdown.replace(
    WELL_FORMED_WIKI_SEGMENT,
    (fullMatch: string, inner: string | undefined) => {
      const raw = inner ?? ""
      if (!isValidPropertyWikiInner(raw)) {
        return fullMatch
      }
      return splitWikiLinkInner(raw).display
    }
  )
}
