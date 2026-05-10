import type {
  Folder,
  FolderRealm,
  NotebookRealm,
} from "@generated/doughnut-backend-api"
import { ref, shallowRef, type Ref, type ShallowRef } from "vue"

/** Set on `notebookPage` from {@link NotebookController.get} for layout chrome before a note realm exists. */
export const notebookSidebarNotebookRealm: ShallowRef<
  NotebookRealm | undefined
> = shallowRef(undefined)

export const currentNotebookId: Ref<number | undefined> = ref(undefined)
export const currentActiveNoteId: Ref<number | undefined> = ref(undefined)

/** Mirrors Sidebar's user-selected folder for create-note scope (notebook layout). */
export const notebookSidebarActiveFolder: Ref<FolderRealm | null> = ref(null)

/** Root-to-leaf folder segments for `folderPage` breadcrumbs (see flat index walk). */
export const folderPageBreadcrumbFolders: Ref<Folder[]> = ref([])

/** Latest folder page payload while `folderPage` route is active (for folder index id, etc.). */
export const folderSidebarFolderRealm: ShallowRef<FolderRealm | undefined> =
  shallowRef(undefined)

export function resetNotebookSidebarState(): void {
  currentNotebookId.value = undefined
  currentActiveNoteId.value = undefined
  notebookSidebarNotebookRealm.value = undefined
  notebookSidebarActiveFolder.value = null
  folderPageBreadcrumbFolders.value = []
  folderSidebarFolderRealm.value = undefined
}
