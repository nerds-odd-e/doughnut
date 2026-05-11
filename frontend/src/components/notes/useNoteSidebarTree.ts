import type { FolderRealm, NoteRealm } from "@generated/doughnut-backend-api"
import { type ComputedRef, type InjectionKey, type Ref } from "vue"

/** Leaf folder for placement UI: last segment of `ancestorFolders` (notebook root when absent). */
export function realmLeafFolder(realm: NoteRealm | undefined) {
  const chain = realm?.ancestorFolders
  if (chain == null || chain.length === 0) return
  return chain[chain.length - 1]
}

export interface SidebarTreeContext {
  expandedFolderIds: Ref<Set<number>>
  activePathFolderIds: ComputedRef<Set<number>>
  activeFolder: Ref<FolderRealm | null>
}

export const sidebarTreeKey: InjectionKey<SidebarTreeContext> =
  Symbol("sidebarTree")
