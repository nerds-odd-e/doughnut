import type { NoteRealm } from "@generated/doughnut-backend-api"
import { computed, type ComputedRef, type InjectionKey, type Ref } from "vue"

/** Folder the user explicitly selected in the sidebar tree (new note / new folder scope). */
export type SidebarUserActiveFolder = { id: number; name: string }

export function folderLabelForRealmFolderId(
  realm: NoteRealm | undefined,
  folderId: number
): string {
  const seg = realm?.ancestorFolders?.find((s) => s.id === folderId)
  return seg?.name ?? `Folder #${folderId}`
}

export function resolvedCreateParentFolderIdFrom(
  userActiveFolder: SidebarUserActiveFolder | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): number | null {
  if (userActiveFolder != null) return userActiveFolder.id
  const fid = activeNoteRealm?.note?.noteTopology?.folderId
  if (fid != null && noteContextResolved) return fid
  return null
}

export function createParentLocationDescriptionFrom(
  userActiveFolder: SidebarUserActiveFolder | null,
  activeNoteRealm: NoteRealm | undefined,
  noteContextResolved: boolean
): string {
  if (userActiveFolder != null) {
    const label =
      userActiveFolder.name !== ""
        ? userActiveFolder.name
        : folderLabelForRealmFolderId(activeNoteRealm, userActiveFolder.id)
    return `Adds to folder "${label}".`
  }
  if (!noteContextResolved) return "Adds to the notebook root."
  const fid = activeNoteRealm?.note?.noteTopology?.folderId
  if (fid == null) return "Adds to the notebook root."
  const label = folderLabelForRealmFolderId(activeNoteRealm, fid)
  return `Adds to folder "${label}".`
}

export function useNotebookRootCreateTarget(
  userActiveFolder: Ref<SidebarUserActiveFolder | null>,
  activeNoteRealm: Ref<NoteRealm | undefined>,
  noteContextResolved: Ref<boolean>
): {
  resolvedCreateParentFolderId: ComputedRef<number | null>
  createParentLocationDescription: ComputedRef<string>
} {
  const resolvedCreateParentFolderId = computed(() =>
    resolvedCreateParentFolderIdFrom(
      userActiveFolder.value,
      activeNoteRealm.value,
      noteContextResolved.value
    )
  )
  const createParentLocationDescription = computed(() =>
    createParentLocationDescriptionFrom(
      userActiveFolder.value,
      activeNoteRealm.value,
      noteContextResolved.value
    )
  )
  return { resolvedCreateParentFolderId, createParentLocationDescription }
}

export interface SidebarTreeContext {
  expandedFolderIds: Ref<Set<number>>
  toggleFolder: (id: number) => void
  ancestorFolderIds: ComputedRef<Set<number>>
  activeNoteFolderIds: ComputedRef<Set<number>>
  userActiveFolder: Ref<SidebarUserActiveFolder | null>
}

export const sidebarTreeKey: InjectionKey<SidebarTreeContext> =
  Symbol("sidebarTree")
