/**
 * epub.js spine `href` is often manifest-relative (e.g. `chapter1.xhtml`) while our API stores
 * package-root paths (e.g. `OEBPS/chapter1.xhtml`).
 */
export function epubSpinePathMatches(
  storedPath: string,
  relocatedPath: string
): boolean {
  const a = storedPath.replace(/^\/+/, "").trim()
  const b = relocatedPath.replace(/^\/+/, "").trim()
  if (a === b) {
    return true
  }
  return a.endsWith(`/${b}`) || b.endsWith(`/${a}`)
}

export function splitEpubHref(href: string): {
  path: string
  fragment: string | null
} {
  const i = href.indexOf("#")
  if (i === -1) {
    return { path: href, fragment: null }
  }
  const fragment = href.slice(i + 1)
  return {
    path: href.slice(0, i),
    fragment: fragment.length === 0 ? null : fragment,
  }
}

/**
 * Find a spine section href whose path matches the given stored path. Our API stores
 * package-root paths (e.g. `OEBPS/chapter1.xhtml`) while epub.js's spine is indexed by
 * the raw OPF manifest href (e.g. `chapter1.xhtml`), so `rendition.display(storedPath)`
 * would otherwise fail with "No Section Found".
 */
export function resolveSpineHrefForStoredPath(
  spineItems: ReadonlyArray<{ href?: string }> | undefined,
  storedPath: string
): string | null {
  const stored = storedPath.trim()
  if (stored.length === 0 || !spineItems) {
    return null
  }
  for (const item of spineItems) {
    const href = item.href?.trim()
    if (!href) {
      continue
    }
    if (epubSpinePathMatches(stored, href)) {
      return href
    }
  }
  return null
}
