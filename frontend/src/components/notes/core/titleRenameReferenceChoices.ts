import type { TitleRenameReferenceHandling } from "@/store/noteStore"

export const TITLE_RENAME_REFERENCE_PROMPT =
  "This note is linked from other notes. Choose how wiki links to this note should change:"

export const TITLE_RENAME_REFERENCE_SAVE_OPTIONS: {
  value: TitleRenameReferenceHandling
  label: string
  testid: string
}[] = [
  {
    value: "UPDATE_VISIBLE_TEXT",
    label: "Update visible reference text",
    testid: "referenced-title-save-update-visible-text",
  },
  {
    value: "KEEP_VISIBLE_TEXT",
    label: "Keep visible reference text",
    testid: "referenced-title-save-keep-visible-text",
  },
]
