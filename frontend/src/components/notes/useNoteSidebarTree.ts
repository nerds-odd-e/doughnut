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

export function resolvedCreateParentFolderFrom(
  activeFolder: FolderRealm | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): ResolvedCreateParentFolder | null {
  if (activeFolder != null) return activeFolder
  const leaf = realmLeafFolder(activeNoteRealm)
  if (leaf != null && noteContextResolved) {
    return leaf
  }
  return null
}

/** Folder row for forms that only accept {@link Folder} (not full folder page payload). */
function folderFromResolvedCreateParent(
  v: ResolvedCreateParentFolder | null | undefined
): Folder | undefined {
  if (v == null) return
  return "folder" in v ? v.folder : v
}

export function resolvedFolderIdFromPageOrFolder(
  v: ResolvedCreateParentFolder | null
): number | null {
  return folderFromResolvedCreateParent(v)?.id ?? null
}

export function resolvedCreateParentFolderIdFrom(
  activeFolder: FolderRealm | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): number | null {
  return (
    resolvedFolderIdFromPageOrFolder(
      resolvedCreateParentFolderFrom(
        activeFolder,
        activeNoteRealm,
        noteContextResolved
      )
    ) ?? null
  )
}

export function createParentLocationDescriptionFrom(
  activeFolder: FolderRealm | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): string {
  if (activeFolder != null) {
    return `Adds to folder "${activeFolder.folder.name}".`
  }
  if (!noteContextResolved) return "Adds to the notebook root."
  const leaf = realmLeafFolder(activeNoteRealm)
  if (leaf == null) return "Adds to the notebook root."
  return `Adds to folder "${leaf.name}".`
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
  const resolvedCreateParentFolder = computed(() =>
    resolvedCreateParentFolderFrom(
      activeFolder.value,
      activeNoteRealm.value,
      noteContextResolved.value
    )
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
  const createParentLocationDescription = computed(() =>
    createParentLocationDescriptionFrom(
      activeFolder.value,
      activeNoteRealm.value,
      noteContextResolved.value
    )
  )
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
