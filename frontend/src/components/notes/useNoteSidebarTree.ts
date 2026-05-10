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
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): number | null {
  let parent: ResolvedCreateParentFolder | null = null
  if (activeFolder != null) {
    parent = activeFolder
  } else {
    const leaf = realmLeafFolder(activeNoteRealm)
    if (leaf != null && noteContextResolved) {
      parent = leaf
    }
  }
  return folderFromResolvedCreateParent(parent)?.id ?? null
}

export function useNotebookRootCreateTarget(
  activeFolder: Ref<FolderRealm | null>,
  activeNoteRealm: Ref<NoteRealm | undefined>,
  noteContextResolved: Ref<boolean>
): {
  resolvedCreateParentFolder: ComputedRef<ResolvedCreateParentFolder | null>
  /** Same scope as {@link resolvedCreateParentFolder}, normalized to a {@link Folder} row for forms. */
  resolvedCreateParentFolderRow: ComputedRef<Folder | null>
  resolvedCreateParentFolderId: ComputedRef<number | null>
  createParentLocationDescription: ComputedRef<string>
} {
  const resolvedCreateParentFolder = computed(
    (): ResolvedCreateParentFolder | null => {
      const af = activeFolder.value
      const ar = activeNoteRealm.value
      const ncr = noteContextResolved.value
      if (af != null) return af
      const leaf = realmLeafFolder(ar)
      if (leaf != null && ncr) return leaf
      return null
    }
  )
  const resolvedCreateParentFolderRow = computed(
    (): Folder | null =>
      folderFromResolvedCreateParent(resolvedCreateParentFolder.value) ?? null
  )
  const resolvedCreateParentFolderId = computed(() =>
    resolvedCreateParentFolderIdFrom(
      activeFolder.value,
      activeNoteRealm.value,
      noteContextResolved.value
    )
  )
  const createParentLocationDescription = computed(() => {
    const af = activeFolder.value
    const ar = activeNoteRealm.value
    const ncr = noteContextResolved.value
    if (af != null) {
      return `Adds to folder "${af.folder.name}".`
    }
    if (!ncr) return "Adds to the notebook root."
    const leaf = realmLeafFolder(ar)
    if (leaf == null) return "Adds to the notebook root."
    return `Adds to folder "${leaf.name}".`
  })
  return {
    resolvedCreateParentFolder,
    resolvedCreateParentFolderRow,
    resolvedCreateParentFolderId,
    createParentLocationDescription,
  }
}

export interface SidebarTreeContext {
  expandedFolderIds: Ref<Set<number>>
  activePathFolderIds: ComputedRef<Set<number>>
  activeFolder: Ref<FolderRealm | null>
}

export const sidebarTreeKey: InjectionKey<SidebarTreeContext> =
  Symbol("sidebarTree")
