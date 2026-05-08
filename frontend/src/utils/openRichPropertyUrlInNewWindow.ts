/** Opens a rich-mode `url` property value in a new browser tab (http(s), //, or https-prefixed). */
export function openRichPropertyUrlInNewWindow(raw: string) {
  const t = raw.trim()
  if (!t) return
  let href = t
  if (href.startsWith("//")) {
    window.open(`https:${href}`, "_blank", "noopener,noreferrer")
    return
  }
  if (!/^https?:\/\//i.test(href)) {
    href = `https://${href}`
  }
  window.open(href, "_blank", "noopener,noreferrer")
}
