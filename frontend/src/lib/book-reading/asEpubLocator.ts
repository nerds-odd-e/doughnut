import type {
  BookBlockFull,
  EpubLocatorFull,
} from "@generated/doughnut-backend-api"

export function asEpubLocator(
  loc: BookBlockFull["contentLocators"][number] | undefined
): EpubLocatorFull | null {
  if (!loc) {
    return null
  }
  const tag = loc.type as string
  if (tag === "EpubLocator_Full" || tag === "epub") {
    return loc as EpubLocatorFull
  }
  return null
}

export function epubDisplayHref(loc: EpubLocatorFull): string {
  const href = loc.href.trim()
  const frag = loc.fragment?.trim() ?? ""
  return href + frag
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
