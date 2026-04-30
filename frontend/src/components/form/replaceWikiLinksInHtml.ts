import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowByNotebookSlugHref } from "@/routes/noteShowLocation"

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
  result = result.replace(
    /\[\[([^\]]+)\]\]/g,
    (_match, title) => `<a href="#" class="dead-link">${title}</a>`
  )
  return result
}
