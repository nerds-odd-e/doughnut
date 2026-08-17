import {
  isValidWikiLinkInner,
  splitWikiLinkInner,
} from "@/utils/wikiLinkMarkup"

const WELL_FORMED_WIKI_SEGMENT = /\[\[([^\[\]\r\n]*)\]\]/g

function unescapeLiteralNewlines(text: string): string {
  return text
    .replace(/\\r\\n/g, "\n")
    .replace(/\\n/g, "\n")
    .replace(/\\r/g, "\n")
}

function replaceWellFormedWikiLinksWithDisplayPlain(markdown: string): string {
  return markdown.replace(
    WELL_FORMED_WIKI_SEGMENT,
    (fullMatch: string, inner: string | undefined) => {
      const raw = inner ?? ""
      if (!isValidWikiLinkInner(raw)) {
        return fullMatch
      }
      return splitWikiLinkInner(raw).display
    }
  )
}

/** Normalize AI-escaped newlines and strip well-formed wiki links before markdown render. */
export function prepareQuestionStemMarkdown(markdown: string): string {
  return replaceWellFormedWikiLinksWithDisplayPlain(
    unescapeLiteralNewlines(markdown)
  )
}
