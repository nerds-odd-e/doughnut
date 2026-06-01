import { computed, type Ref } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

export function useRelocatedNoteCreationInMainColumn(
  sidebarOpened: Ref<boolean>,
  route: RouteLocationNormalizedLoaded,
  currentNotebookId: Ref<number | undefined>,
  noteCreationReadonly: Ref<boolean>
) {
  return computed(() => {
    if (sidebarOpened.value) return false
    if (currentNotebookId.value == null) return false
    if (noteCreationReadonly.value) return false
    const name = route.name
    return name === "notebookPage" || name === "folderPage"
  })
}
