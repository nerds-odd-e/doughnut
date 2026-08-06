import markdownizer from "@/components/form/markdownizer"

type MarkdownToHtmlOptions = Parameters<typeof markdownizer.markdownToHtml>[1]

export const toHtml = (
  markdown: string | undefined,
  options?: MarkdownToHtmlOptions
) => markdownizer.markdownToHtml(markdown, options)

export const toHtmlElement = (
  markdown: string,
  options?: MarkdownToHtmlOptions
) => {
  const div = document.createElement("div")
  div.innerHTML = toHtml(markdown, options)
  return div
}
