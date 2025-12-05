import markdownizer from "./markdownizer"

export async function handleHtmlPaste(
  event: ClipboardEvent,
  callback: (markdown: string) => void
): Promise<boolean> {
  const htmlData = event.clipboardData?.getData("text/html")
  if (htmlData) {
    event.preventDefault()
    const markdown = markdownizer.htmlToMarkdown(htmlData)
    callback(markdown)
    return true
  }
  return false
}
