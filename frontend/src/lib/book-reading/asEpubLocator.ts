import type {
  BookBlockFull,
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/doughnut-backend-api"

export function asEpubLocator(
  loc: ContentLocatorFull | undefined
): EpubLocatorFull | null {
  if (!loc) {
    return null
  }
  if (loc.type === "EpubLocator_Full") {
    return loc as EpubLocatorFull
  }
  return null
}

export function epubDisplayHref(loc: EpubLocatorFull): string {
  const href = loc.href.trim()
  const frag = loc.fragment?.trim() ?? ""
  return frag.length === 0 ? href : `${href}#${frag}`
}

export function blockStartEpubDisplayHref(
  block: Pick<BookBlockFull, "contentLocators">
): string | null {
  const first = asEpubLocator(block.contentLocators[0])
  if (!first) {
    return null
  }
  const s = epubDisplayHref(first)
  return s.length > 0 ? s : null
}
