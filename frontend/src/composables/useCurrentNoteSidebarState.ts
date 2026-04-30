import type { Notebook } from "@generated/doughnut-backend-api"
import { ref, shallowRef, type Ref, type ShallowRef } from "vue"

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

export function resetNotebookSidebarState(): void {
  currentNotebookId.value = undefined
  currentActiveNoteId.value = undefined
  notebookSidebarNotebookPageContext.value = undefined
}
