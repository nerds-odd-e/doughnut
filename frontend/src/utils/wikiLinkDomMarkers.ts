/** CSS class on resolved in-app wiki link anchors in rich content. */
export const DONUT_WIKI_LINK_CLASS = "donut-wiki-link"

/** CSS class on confirmed-missing wiki link anchors in rich content. */
export const DEAD_WIKI_LINK_CLASS = "dead-wiki-link"

/** CSS class on wiki link anchors not yet confirmed by the last persisted snapshot. */
export const PENDING_WIKI_LINK_CLASS = "pending-wiki-link"

/** Transient in-editor marker: the wiki link's Portable path. */
export const WIKI_LINK_PORTABLE_PATH_ATTR = "data-portable-path"

/** Transient in-editor marker: display text when it differs from the Portable path. */
export const WIKI_LINK_DISPLAY_TEXT_ATTR = "data-display-text"

/** Transient in-editor marker: wiki-link resolution when it is not a live destination. */
export const WIKI_LINK_RESOLUTION_ATTR = "data-resolution"

/** True when the element carries a live, dead, or pending wiki-link class. */
export function isWikiLinkAnchor(el: Element): boolean {
  return (
    el.classList.contains(DONUT_WIKI_LINK_CLASS) ||
    el.classList.contains(DEAD_WIKI_LINK_CLASS) ||
    el.classList.contains(PENDING_WIKI_LINK_CLASS)
  )
}
