import Quill, { type QuillOptions, type Range } from "quill"
import { donutQuillBrMatcher } from "./registerDonutQuillBlots"

// Shift+Enter handler for soft line breaks
const shiftEnterHandler = function (
  this: { quill: Quill },
  range: Range | null
) {
  if (!range) return
  this.quill.insertEmbed(range.index, "softbreak", true, Quill.sources.USER)
  this.quill.insertText(range.index + 1, "\u200B", Quill.sources.USER)
  this.quill.setSelection(range.index + 1, Quill.sources.SILENT)
}

const toolbarRows = [
  ["bold", "italic", "underline", "code"],
  [{ header: 1 }, { header: 2 }],
  ["blockquote", "code-block"],
  [{ list: "ordered" }, { list: "bullet" }],
  ["link"],
]

export const donutQuillOptions = (
  readonly: boolean,
  placeholder: string
): QuillOptions => ({
  modules: {
    toolbar: readonly ? false : toolbarRows,
    keyboard: {
      bindings: {
        shiftEnter: {
          key: "Enter",
          shiftKey: true,
          handler: shiftEnterHandler,
        },
      },
    },
    clipboard: {
      matchers: [["BR", donutQuillBrMatcher]],
      matchVisual: false,
    },
  },
  formats: [
    "bold",
    "italic",
    "underline",
    "code",
    "header",
    "blockquote",
    "code-block",
    "list",
    "indent",
    "link",
    "image",
    "mark",
    "softbreak",
    "horizontalrule",
    "table",
  ],
  placeholder: readonly ? "" : placeholder,
  readOnly: readonly,
  theme: "bubble",
})
