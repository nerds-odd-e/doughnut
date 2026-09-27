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
 * path changes) and clears it otherwise. Returns the reload function and failed flag.
 */
function loadRealmOnRoute<T>(
  route: RouteLocationNormalizedLoaded,
  routeName: string,
  realm: Ref<T | undefined>,
  fetchRealm: () => Promise<{ data?: T; error?: unknown }>
) {
  const failed = ref(false)
  async function reload() {
    failed.value = false
    const { data, error } = await fetchRealm()
    if (error) {
      failed.value = true
      realm.value = undefined
      return
    }
    realm.value = data
  }
  watch(
    () => (route.name === routeName ? route.path : undefined),
    async (path) => {
      if (path === undefined) {
        realm.value = undefined
        failed.value = false
        return
      }
      await reload()
    },
    { immediate: true }
  )
  return { reload, failed }
}

export function useNotebookSidebarRouteRealms(
  route: RouteLocationNormalizedLoaded
) {
  const activeNotebookRealm = ref<NotebookRealm | undefined>(undefined)
  const activeFolderRealm = ref<FolderRealm | undefined>(undefined)
  const activeAttachmentRealm = ref<NotebookAttachmentRealm | undefined>(
    undefined
  )

  const { reload: fetchNotebookPage, failed: notebookLoadFailed } =
    loadRealmOnRoute(route, "notebookPage", activeNotebookRealm, () =>
      NotebookController.get({
        path: { notebook: Number(route.params.notebookId) },
      })
    )

  const { reload: fetchFolderPage, failed: folderLoadFailed } =
    loadRealmOnRoute(route, "folderPage", activeFolderRealm, () =>
      apiCallWithLoading(() =>
        NotebookFolderController.getFolderPage({
          path: {
            notebook: Number(route.params.notebookId),
            folder: Number(route.params.folderId),
          },
        })
      )
    )

  const { failed: attachmentLoadFailed } = loadRealmOnRoute(
    route,
    "attachmentPage",
    activeAttachmentRealm,
    () =>
      apiCallWithLoading(() =>
        NotebookAttachmentController.getAttachmentPage({
          path: {
            notebook: Number(route.params.notebookId),
            attachment: Number(route.params.attachmentId),
          },
        })
      )
  )

  const pageLoadFailed = computed(
    () =>
      (route.name === "notebookPage" && notebookLoadFailed.value) ||
      (route.name === "folderPage" && folderLoadFailed.value) ||
      (route.name === "attachmentPage" && attachmentLoadFailed.value)
  )

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
    pageLoadFailed,
  }
}
