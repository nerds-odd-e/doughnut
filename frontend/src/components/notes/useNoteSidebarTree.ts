import type { NoteRealm } from "@generated/doughnut-backend-api"
import { computed, type ComputedRef, type InjectionKey, type Ref } from "vue"

/** Folder the user explicitly selected in the sidebar tree (new note / new folder scope). */
export type SidebarActiveFolder = { id: number; name: string }

function realmLeafFolder(realm: NoteRealm | undefined) {
  const chain = realm?.ancestorFolders
  if (chain == null || chain.length === 0) return
  return chain[chain.length - 1]
}

export function resolvedCreateParentFolderFrom(
  activeFolder: SidebarActiveFolder | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): SidebarActiveFolder | null {
  if (activeFolder != null) return activeFolder
  const leaf = realmLeafFolder(activeNoteRealm)
  if (leaf != null && noteContextResolved) {
    return leaf
  }
  return null
}

export function resolvedCreateParentFolderIdFrom(
  activeFolder: SidebarActiveFolder | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): number | null {
  return (
    resolvedCreateParentFolderFrom(
      activeFolder,
      activeNoteRealm,
      noteContextResolved
    )?.id ?? null
  )
}

export function createParentLocationDescriptionFrom(
  activeFolder: SidebarActiveFolder | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): string {
  if (activeFolder != null) {
    return `Adds to folder "${activeFolder.name}".`
  }
  if (!noteContextResolved) return "Adds to the notebook root."
  const leaf = realmLeafFolder(activeNoteRealm)
  if (leaf == null) return "Adds to the notebook root."
  const label = leaf.name !== "" ? leaf.name : `Folder #${leaf.id}`
  return `Adds to folder "${label}".`
}

export function useNotebookRootCreateTarget(
  activeFolder: Ref<SidebarActiveFolder | null>,
  activeNoteRealm: Ref<NoteRealm | undefined>,
  noteContextResolved: Ref<boolean>
): {
  resolvedCreateParentFolder: ComputedRef<SidebarActiveFolder | null>
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
  const resolvedCreateParentFolderId = computed(
    () => resolvedCreateParentFolder.value?.id ?? null
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
    resolvedCreateParentFolderId,
    createParentLocationDescription,
  }
}

export interface SidebarTreeContext {
  expandedFolderIds: Ref<Set<number>>
  activePathFolderIds: ComputedRef<Set<number>>
  activeFolder: Ref<SidebarActiveFolder | null>
}

export const sidebarTreeKey: InjectionKey<SidebarTreeContext> =
  Symbol("sidebarTree")
