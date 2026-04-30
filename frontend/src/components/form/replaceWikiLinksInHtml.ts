import type { WikiTitle } from "@generated/doughnut-backend-api"

export function replaceWikiLinksInHtml(
  html: string,
  wikiTitles: WikiTitle[]
): string {
  let result = html
  wikiTitles.forEach(({ title, noteId }) => {
    result = result.replace(
      `[[${title}]]`,
      `<a href="/n${noteId}" class="doughnut-link">${title}</a>`
    )
  })
  result = result.replace(
    /\[\[([^\]]+)\]\]/g,
    (_match, title) => `<a href="#" class="dead-link">${title}</a>`
  )
  return result
}
