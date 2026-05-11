import type {
  Folder,
  FolderRealm,
  NoteRealm,
} from "@generated/doughnut-backend-api"
import { computed, type ComputedRef, type InjectionKey, type Ref } from "vue"

/** Resolved parent folder for create-note / new-folder (sidebar folder page, else realm leaf). */
export type ResolvedCreateParentFolder = FolderRealm | Folder

/** Leaf folder for placement UI: last segment of `ancestorFolders` (notebook root when absent). */
export function realmLeafFolder(realm: NoteRealm | undefined) {
  const chain = realm?.ancestorFolders
  if (chain == null || chain.length === 0) return
  return chain[chain.length - 1]
}

/** Folder row for forms that only accept {@link Folder} (not full folder page payload). */
function folderFromResolvedCreateParent(
  v: ResolvedCreateParentFolder | null | undefined
): Folder | undefined {
  if (v == null) return
  return "folder" in v ? v.folder : v
}

export function resolvedCreateParentFolderIdFrom(
  activeFolder: FolderRealm | null,
  activeNoteRealm: NoteRealm | undefined
): number | null {
  let parent: ResolvedCreateParentFolder | null = null
  if (activeFolder != null) {
    parent = activeFolder
  } else {
    parent = realmLeafFolder(activeNoteRealm) ?? null
  }
  return folderFromResolvedCreateParent(parent)?.id ?? null
}

export function useNotebookRootCreateTarget(
  activeFolder: Ref<FolderRealm | null>,
  activeNoteRealm: Ref<NoteRealm | undefined>
): {
  resolvedCreateParentFolderRow: ComputedRef<Folder | null>
} {
  const resolvedCreateParentFolder = computed(
    (): ResolvedCreateParentFolder | null => {
      const af = activeFolder.value
      const ar = activeNoteRealm.value
      if (af != null) return af
      const leaf = realmLeafFolder(ar)
      return leaf ?? null
    }
  )

  const resolvedCreateParentFolderRow = computed(
    (): Folder | null =>
      folderFromResolvedCreateParent(resolvedCreateParentFolder.value) ?? null
  )

  return {
    resolvedCreateParentFolderRow,
  }
}

export interface SidebarTreeContext {
  expandedFolderIds: Ref<Set<number>>
  activePathFolderIds: ComputedRef<Set<number>>
  activeFolder: Ref<FolderRealm | null>
}

export const sidebarTreeKey: InjectionKey<SidebarTreeContext> =
  Symbol("sidebarTree")
