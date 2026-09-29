import type {
  BookBlockFull,
  ContentLocatorFull,
  EpubLocatorFull,
} from "@generated/donut-backend-api"

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

export function blockStartEpubDisplayHref(
  block: Pick<BookBlockFull, "contentLocators">
): string | null {
  const first = asEpubLocator(block.contentLocators[0])
  if (!first) {
    return null
  }
  const href = first.href.trim()
  const frag = first.fragment?.trim() ?? ""
  const s = frag.length === 0 ? href : `${href}#${frag}`
  return s.length > 0 ? s : null
}
