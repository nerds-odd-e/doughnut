import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowByNotebookSlugHref } from "@/routes/noteShowLocation"

function escapeRegExp(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&")
}

/** Rich editor HTML uses dead-link anchors, not [[ ]] literals; upgrade when titles resolve. */
function upgradeDeadWikiAnchors(html: string, wikiTitles: WikiTitle[]): string {
  let result = html
  for (const { linkText, notebookId, slug } of wikiTitles) {
    const esc = escapeRegExp(linkText)
    const href = noteShowByNotebookSlugHref(notebookId, slug)
    const live = `<a href="${href}" class="doughnut-link">${linkText}</a>`
    result = result.replace(
      new RegExp(`<a href="#" class="dead-link">\\s*${esc}\\s*</a>`, "gi"),
      () => live
    )
    result = result.replace(
      new RegExp(`<a class="dead-link" href="#">\\s*${esc}\\s*</a>`, "gi"),
      () => live
    )
  }
  return result
}

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  wikiTitles.forEach(({ linkText, notebookId, slug }) => {
    result = result.replace(
      `[[${linkText}]]`,
      `<a href="${noteShowByNotebookSlugHref(notebookId, slug)}" class="doughnut-link">${linkText}</a>`
    )
  })
  result = upgradeDeadWikiAnchors(result, wikiTitles)
  result = result.replace(
    /\[\[([^\]]+)\]\]/g,
    (_match, title) => `<a href="#" class="dead-link">${title}</a>`
  )
  return result
}
