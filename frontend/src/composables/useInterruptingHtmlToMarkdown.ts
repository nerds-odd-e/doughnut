import markdownizer from "@/components/form/markdownizer"

export function useInterruptingHtmlToMarkdown() {
  const htmlToMarkdown = (html: string) => {
    let markdown = markdownizer.htmlToMarkdown(html)

    // Count markdown links: [text](url)
    const linkRegex = /\[([^\]]+)\]\(([^)]+)\)/g
    const links = markdown.match(linkRegex)
    const linkCount = links ? links.length : 0

    if (linkCount > 2) {
      const confirmed = window.confirm(
        `Shall I remove the ${linkCount} links from the pasting content?`
      )

      if (confirmed) {
        // Replace [text](url) with just text
        markdown = markdown.replace(linkRegex, "$1")
      }
    }

    return markdown
  }

  return {
    htmlToMarkdown,
  }
}
