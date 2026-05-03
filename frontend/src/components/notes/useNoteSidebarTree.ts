import type { ComputedRef, InjectionKey, Ref } from "vue"

export interface SidebarTreeContext {
  expandedFolderIds: Ref<Set<number>>
  toggleFolder: (id: number) => void
  ancestorFolderIds: ComputedRef<Set<number>>
  activeNoteFolderIds: ComputedRef<Set<number>>
  activeNoteTitle: ComputedRef<string | null>
  userActiveFolderId: Ref<number | null>
}

export const sidebarTreeKey: InjectionKey<SidebarTreeContext> =
  Symbol("sidebarTree")
