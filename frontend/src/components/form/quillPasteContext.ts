import type Quill from "quill"
import type { Range } from "quill"
import markdownizer from "./markdownizer"

/**
 * Original clipboard text and the Quill range a rich paste replaced, captured
 * from `quill.getSelection()` before Quill's own clipboard module mutates the
 * document (real positions, not re-derived by searching converted Markdown
 * strings). `insertedLength` is the length of what Quill inserted, taken from
 * the applied Delta's total length rather than a post-paste selection read,
 * since Quill moves the selection with a SILENT source that QuillEditor.vue's
 * own selection-change listener does not observe.
 */
export type QuillPasteContext = {
  originalText: string
  range: { index: number; length: number }
  insertedLength: number
}

/** Routes the pasted HTML through Markdown, so Quill only takes in what a note
 * can keep, and captures the paste's context before Quill applies it. */
export const interceptRichPaste = (
  quill: Pick<Quill, "getSelection">,
  clipboardData: DataTransfer
): Omit<QuillPasteContext, "insertedLength"> | null => {
  const originalGetData = clipboardData.getData.bind(clipboardData)

  // Quill's own getSelection() can throw when the browser's native
  // selection doesn't map onto a blot (e.g. no real caret was ever
  // placed); when that happens there is simply no paste context to
  // capture, matching QuillEditor.vue's insertTextAtCursor precedent.
  let range: Range | null = null
  try {
    range = quill.getSelection(true)
  } catch {
    range = null
  }

  clipboardData.getData = (format: string) => {
    if (format === "text/html") {
      const htmlData = originalGetData(format)
      if (htmlData) {
        const markdown = markdownizer.htmlToMarkdown(htmlData)
        return markdownizer.markdownToHtml(markdown, { preserve_pre: true })
      }
    }
    return originalGetData(format)
  }

  return range
    ? {
        originalText: originalGetData("text/plain"),
        range: { index: range.index, length: range.length },
      }
    : null
}
