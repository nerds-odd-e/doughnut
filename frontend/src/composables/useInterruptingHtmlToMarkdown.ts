import markdownizer from "@/components/form/markdownizer"

export function useInterruptingHtmlToMarkdown() {
  const htmlToMarkdown = (html: string) => {
    return markdownizer.htmlToMarkdown(html)
  }

  return {
    htmlToMarkdown,
  }
}
