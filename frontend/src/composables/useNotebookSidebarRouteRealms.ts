import type {
  FolderRealm,
  NotebookRealm,
} from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, ref, watch } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

export function useNotebookSidebarRouteRealms(
  route: RouteLocationNormalizedLoaded
) {
  const activeNotebookRealm = ref<NotebookRealm | undefined>(undefined)
  const activeFolderRealm = ref<FolderRealm | undefined>(undefined)

  async function fetchNotebookPage() {
    const notebookId = Number(route.params.notebookId)
    const { data, error } = await NotebookController.get({
      path: { notebook: notebookId },
    })
    activeNotebookRealm.value = !error && data ? data : undefined
  }

  async function fetchFolderPage() {
    const notebookId = Number(route.params.notebookId)
    const folderId = Number(route.params.folderId)
    const { data: page, error } = await NotebookController.getFolderPage({
      path: { notebook: notebookId, folder: folderId },
    })
    if (!error && page?.notebookRealm?.notebook) {
      activeFolderRealm.value = page
      return
    }
    activeFolderRealm.value = undefined
  }

  watch(
    () => ({
      isNotebookPage: route.name === "notebookPage",
      notebookId: route.params.notebookId,
    }),
    async ({ isNotebookPage }) => {
      if (!isNotebookPage) {
        activeNotebookRealm.value = undefined
        return
      }
      await fetchNotebookPage()
    },
    { immediate: true }
  )

  watch(
    () => ({
      isFolderPage: route.name === "folderPage",
      notebookId: route.params.notebookId,
      folderId: route.params.folderId,
    }),
    async ({ isFolderPage }) => {
      if (!isFolderPage) {
        activeFolderRealm.value = undefined
        return
      }
      await fetchFolderPage()
    },
    { immediate: true }
  )

  const routeViewProps = computed(() => {
    if (route.name === "notebookPage") {
      return { notebookRealm: activeNotebookRealm.value, fetchNotebookPage }
    }
    if (route.name === "folderPage") {
      return { folderRealm: activeFolderRealm.value, fetchFolderPage }
    }
    return {}
  })

  return {
    activeNotebookRealm,
    activeFolderRealm,
    fetchNotebookPage,
    fetchFolderPage,
    routeViewProps,
  }
}
