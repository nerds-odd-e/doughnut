import type { Folder, FolderRealm } from "@generated/doughnut-backend-api"
import { ref, shallowRef, type Ref, type ShallowRef } from "vue"

/** Root-to-leaf folder segments for `folderPage` breadcrumbs (see flat index walk). */
export const folderPageBreadcrumbFolders: Ref<Folder[]> = ref([])

/** Latest folder page payload while `folderPage` route is active (for folder index id, etc.). */
export const folderSidebarFolderRealm: ShallowRef<FolderRealm | undefined> =
  shallowRef(undefined)

export function resetNotebookSidebarState(): void {
  folderPageBreadcrumbFolders.value = []
  folderSidebarFolderRealm.value = undefined
}
