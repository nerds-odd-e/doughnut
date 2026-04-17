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
