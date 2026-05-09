import type { NotebookPageClientView } from "@generated/doughnut-backend-api"
import { computed, type ComputedRef } from "vue"
import { useRoute } from "vue-router"
import { useStorageAccessor } from "@/composables/useStorageAccessor"
import {
  folderSidebarFolderPageClientView,
  notebookSidebarNotebookClientView,
} from "@/composables/useCurrentNoteSidebarState"
import { titlePatternFromNoteMarkdown } from "@/utils/noteContentFrontmatter"
import { renderTitleFromPattern } from "@/utils/titlePatternRender"

function notebookPageIndexNoteId(): number | undefined {
  const v = notebookSidebarNotebookClientView.value as
    | NotebookPageClientView
    | undefined
  return v?.indexNoteId
}

/** Raw `titlePattern` from the active scoped index (note / notebook / folder page context). */
export function useScopedTitlePatternString(): ComputedRef<string | undefined> {
  const route = useRoute()
  const storageAccessor = useStorageAccessor()

  return computed(() => {
    const name = route.name
    if (name === "noteShow") {
      const id = Number(route.params.noteId)
      if (Number.isNaN(id)) return
      const realm = storageAccessor.value.refOfNoteRealm(id).value
      return titlePatternFromNoteMarkdown(realm?.indexNoteContent ?? null)
    }
    if (name === "notebookPage") {
      const idx = notebookPageIndexNoteId()
      if (idx == null) return
      const ir = storageAccessor.value.refOfNoteRealm(idx).value
      return titlePatternFromNoteMarkdown(ir?.note.content ?? null)
    }
    if (name === "folderPage") {
      const fp = folderSidebarFolderPageClientView.value
      const idx = fp?.folderIndexNoteId
      if (idx == null) return
      const ir = storageAccessor.value.refOfNoteRealm(idx).value
      return titlePatternFromNoteMarkdown(ir?.note.content ?? null)
    }
    return
  })
}

/** Rendered default title for new-note dialog (`{{date}}`, etc.). */
export function useDefaultNewNoteTitleFromPattern(): ComputedRef<
  string | undefined
> {
  const raw = useScopedTitlePatternString()
  return computed(() => {
    const p = raw.value
    if (p == null || p === "") return
    return renderTitleFromPattern(p)
  })
}
