import type { NoteRealm } from "@generated/doughnut-backend-api"
import { computed, type ComputedRef, type InjectionKey, type Ref } from "vue"

/** Folder the user explicitly selected in the sidebar tree (new note / new folder scope). */
export type SidebarActiveFolder = { id: number; name: string }

export function folderLabelForRealmFolderId(
  realm: NoteRealm | undefined,
  folderId: number
): string {
  const seg = realm?.ancestorFolders?.find((s) => s.id === folderId)
  return seg?.name ?? `Folder #${folderId}`
}

export function resolvedCreateParentFolderFrom(
  activeFolder: SidebarActiveFolder | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): SidebarActiveFolder | null {
  if (activeFolder != null) return activeFolder
  const fid = activeNoteRealm?.note?.noteTopology?.folderId
  if (fid != null && noteContextResolved) {
    return {
      id: fid,
      name: folderLabelForRealmFolderId(activeNoteRealm, fid),
    }
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
    const label =
      activeFolder.name !== ""
        ? activeFolder.name
        : folderLabelForRealmFolderId(activeNoteRealm, activeFolder.id)
    return `Adds to folder "${label}".`
  }
  if (!noteContextResolved) return "Adds to the notebook root."
  const fid = activeNoteRealm?.note?.noteTopology?.folderId
  if (fid == null) return "Adds to the notebook root."
  const label = folderLabelForRealmFolderId(activeNoteRealm, fid)
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
