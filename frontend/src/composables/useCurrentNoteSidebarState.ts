import type { Folder } from "@generated/doughnut-backend-api"
import { ref, type Ref } from "vue"

/** Root-to-leaf folder segments for `folderPage` breadcrumbs (see flat index walk). */
export const folderPageBreadcrumbFolders: Ref<Folder[]> = ref([])

export function resetNotebookSidebarState(): void {
  folderPageBreadcrumbFolders.value = []
}
