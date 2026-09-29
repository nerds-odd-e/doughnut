import { provideNotebookSidebarOpened } from "@/composables/notebookSidebarOpened"
import { useSidebarDrawer } from "@/composables/useSidebarDrawer"
import { noteRouteFamilyNoteId } from "@/routes/noteRouteFamily"
import { watch, type Ref } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

export function useNotebookSidebarDrawer(
  route: RouteLocationNormalizedLoaded,
  currentNotebookId: Ref<number | undefined>
) {
  const drawer = useSidebarDrawer()
  provideNotebookSidebarOpened(drawer.opened)

  watch(
    () => [currentNotebookId.value, noteRouteFamilyNoteId(route)],
    drawer.closeOnPhone
  )

  return drawer
}
