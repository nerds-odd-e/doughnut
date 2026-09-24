import type {
  FolderRealm,
  NotebookAttachmentRealm,
  NotebookRealm,
} from "@generated/donut-backend-api"
import {
  NotebookAttachmentController,
  NotebookController,
  NotebookFolderController,
} from "@generated/donut-backend-api/sdk.gen"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { computed, type Ref, ref, watch } from "vue"
import type { RouteLocationNormalizedLoaded } from "vue-router"

/**
 * Loads `realm` while `routeName` is the current route (again whenever its
 * path changes) and clears it otherwise. Returns the reload function.
 */
function loadRealmOnRoute<T>(
  route: RouteLocationNormalizedLoaded,
  routeName: string,
  realm: Ref<T | undefined>,
  fetchRealm: () => Promise<T | undefined>
) {
  async function reload() {
    realm.value = await fetchRealm()
  }
  watch(
    () => (route.name === routeName ? route.path : undefined),
    async (path) => {
      if (path === undefined) {
        realm.value = undefined
        return
      }
      await reload()
    },
    { immediate: true }
  )
  return reload
}

export function useNotebookSidebarRouteRealms(
  route: RouteLocationNormalizedLoaded
) {
  const activeNotebookRealm = ref<NotebookRealm | undefined>(undefined)
  const activeFolderRealm = ref<FolderRealm | undefined>(undefined)
  const activeAttachmentRealm = ref<NotebookAttachmentRealm | undefined>(
    undefined
  )

  const fetchNotebookPage = loadRealmOnRoute(
    route,
    "notebookPage",
    activeNotebookRealm,
    async () => {
      const { data, error } = await NotebookController.get({
        path: { notebook: Number(route.params.notebookId) },
      })
      return !error && data ? data : undefined
    }
  )

  const fetchFolderPage = loadRealmOnRoute(
    route,
    "folderPage",
    activeFolderRealm,
    async () => {
      const { data: page, error } = await apiCallWithLoading(() =>
        NotebookFolderController.getFolderPage({
          path: {
            notebook: Number(route.params.notebookId),
            folder: Number(route.params.folderId),
          },
        })
      )
      return !error && page?.notebookRealm?.notebook ? page : undefined
    }
  )

  loadRealmOnRoute(route, "attachmentPage", activeAttachmentRealm, async () => {
    const { data, error } = await apiCallWithLoading(() =>
      NotebookAttachmentController.getAttachmentPage({
        path: {
          notebook: Number(route.params.notebookId),
          attachment: Number(route.params.attachmentId),
        },
      })
    )
    return error ? undefined : data
  })

  const routeViewProps = computed(() => {
    if (route.name === "notebookPage") {
      return { notebookRealm: activeNotebookRealm.value, fetchNotebookPage }
    }
    if (route.name === "folderPage") {
      return { folderRealm: activeFolderRealm.value, fetchFolderPage }
    }
    if (route.name === "attachmentPage") {
      return { attachmentRealm: activeAttachmentRealm.value }
    }
    return {}
  })

  return {
    activeNotebookRealm,
    activeFolderRealm,
    activeAttachmentRealm,
    fetchNotebookPage,
    fetchFolderPage,
    routeViewProps,
  }
}
