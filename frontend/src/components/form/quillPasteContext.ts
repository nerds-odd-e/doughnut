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
