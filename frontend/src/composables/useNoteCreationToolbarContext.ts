import type {
  Folder,
  FolderRealm,
  Note,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import { realmLeafFolder } from "@/components/notes/useNoteSidebarTree"
import { titlePatternFromNoteMarkdown } from "@/utils/noteContentFrontmatter"
import { renderTitleFromPattern } from "@/utils/titlePatternRender"
import { computed, type MaybeRefOrGetter, toValue } from "vue"

export function useNoteCreationToolbarContext(
  props: MaybeRefOrGetter<{
    activeNoteRealm?: NoteRealm
    activeFolderRealm?: FolderRealm
  }>
) {
  const initialTitle = computed((): string | undefined => {
    const { activeNoteRealm, activeFolderRealm } = toValue(props)
    const markdown =
      activeNoteRealm?.scopedReadmeContent ??
      activeFolderRealm?.scopedReadmeContent ??
      null
    const pattern = titlePatternFromNoteMarkdown(markdown)
    if (pattern == null || pattern === "") return
    return renderTitleFromPattern(pattern)
  })

  const parentFolderForCreation = computed((): Folder | null => {
    const { activeNoteRealm, activeFolderRealm } = toValue(props)
    if (activeFolderRealm) return activeFolderRealm.folder
    return realmLeafFolder(activeNoteRealm) ?? null
  })

  const anchorNote = computed(
    (): Note | undefined => toValue(props).activeNoteRealm?.note
  )

  return { initialTitle, parentFolderForCreation, anchorNote }
}
