import type { WikiTitle } from "@generated/doughnut-backend-api"
import { noteShowByNotebookSlugHref } from "@/routes/noteShowLocation"

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  wikiTitles.forEach(({ title, notebookId, slug }) => {
    result = result.replace(
      `[[${title}]]`,
      `<a href="${noteShowByNotebookSlugHref(notebookId, slug)}" class="doughnut-link">${title}</a>`
    )
  })
  result = result.replace(
    /\[\[([^\]]+)\]\]/g,
    (_match, title) => `<a href="#" class="dead-link">${title}</a>`
  )
  return result
}
