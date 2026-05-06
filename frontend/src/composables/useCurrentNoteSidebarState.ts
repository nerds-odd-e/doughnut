import type { Notebook } from "@generated/doughnut-backend-api"
import { ref, shallowRef, type Ref, type ShallowRef } from "vue"
import type { SidebarUserActiveFolder } from "@/components/notes/useNoteSidebarTree"

/** Set on `notebookPage` so the layout can mirror notebook breadcrumbs without a second notebook fetch */
export const notebookSidebarNotebookPageContext: ShallowRef<
  | {
      notebook: Notebook
      isNotebookReadOnly: boolean
    }
  | undefined
> = shallowRef(undefined)

export const currentNotebookId: Ref<number | undefined> = ref(undefined)
export const currentActiveNoteId: Ref<number | undefined> = ref(undefined)

/** Mirrors Sidebar's user-selected folder for create-note scope (notebook layout). */
export const notebookSidebarUserActiveFolder: Ref<SidebarUserActiveFolder | null> =
  ref(null)

export function resetNotebookSidebarState(): void {
  currentNotebookId.value = undefined
  currentActiveNoteId.value = undefined
  notebookSidebarNotebookPageContext.value = undefined
  notebookSidebarUserActiveFolder.value = null
}
