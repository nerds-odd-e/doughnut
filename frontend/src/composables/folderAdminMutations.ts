import type { Folder, FolderRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import type { Ref } from "vue"
import type { Router } from "vue-router"
import { refreshSidebarStructuralListings } from "@/components/notes/sidebarStructuralRefresh"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"

export function isFolderNameConflict(
  apiError: ReturnType<typeof toOpenApiError>
): boolean {
  return apiError.errorType === "FOLDER_NAME_CONFLICT"
}

export function dissolveParentLabelFromChain(
  movingFolderId: number,
  chain: readonly Folder[]
): string {
  const idx = chain.findIndex((f) => f.id === movingFolderId)
  if (idx <= 0) return "notebook root"
  const parentChain = chain.slice(0, idx)
  return `"${parentChain.map((f) => f.name).join(" / ")}"`
}

export function throwIfSdkError(result: {
  error?: unknown
  response?: { status?: number }
}): void {
  if (!result.error) return
  const httpStatus = result.response?.status
  const err = result.error
  if (typeof httpStatus === "number" && Number.isFinite(httpStatus)) {
    if (typeof err === "object" && err !== null) {
      throw { ...(err as Record<string, unknown>), status: httpStatus }
    }
    throw { message: String(err), status: httpStatus }
  }
  throw err
}

export async function routeAfterFolderDissolve(
  router: Router,
  r: FolderRealm
): Promise<void> {
  const notebookId = r.notebookRealm.notebook.id
  const ancestors = r.ancestorFolders ?? []
  if (ancestors.length === 0) {
    await router.push({
      name: "notebookPage",
      params: { notebookId },
    })
    return
  }
  const parent = ancestors[ancestors.length - 1]!
  await router.push({
    name: "folderPage",
    params: {
      notebookId: String(notebookId),
      folderId: String(parent.id),
    },
  })
}

export function buildFolderMoveBody(options: {
  isCrossNotebookMove: boolean
  destinationNotebookId: number | undefined
  selectedParentFolder: Folder | null
  merge: boolean
}) {
  if (options.isCrossNotebookMove) {
    const body: {
      destinationNotebookId: number | undefined
      newParentFolderId?: number
      merge: boolean
    } = {
      destinationNotebookId: options.destinationNotebookId,
      merge: options.merge,
    }
    if (options.selectedParentFolder != null) {
      body.newParentFolderId = options.selectedParentFolder.id
    }
    return body
  }
  if (options.selectedParentFolder == null) {
    return { merge: options.merge }
  }
  return {
    newParentFolderId: options.selectedParentFolder.id,
    merge: options.merge,
  }
}

export type FolderAdminConfirm = (message: string) => Promise<boolean>

export async function renameFolderOnPage(options: {
  folderRealm: FolderRealm
  newName: string
  fetchFolderPage: () => Promise<void>
  renameError: Ref<string | undefined>
}): Promise<void> {
  options.renameError.value = undefined
  const renameResult = await apiCallWithLoading(() =>
    NotebookController.renameFolder({
      path: {
        notebook: options.folderRealm.notebookRealm.notebook.id,
        folder: options.folderRealm.folder.id,
      },
      body: { name: options.newName },
    })
  )
  throwIfSdkError(renameResult)
  refreshSidebarStructuralListings()
  await options.fetchFolderPage()
}

export async function moveFolderOnPage(options: {
  folderRealm: FolderRealm
  body: ReturnType<typeof buildFolderMoveBody>
  destinationNotebookId: number
  isCrossNotebookMove: boolean
  fetchFolderPage: () => Promise<void>
  router: Router
  confirm: FolderAdminConfirm
  moveError: Ref<string | undefined>
  onConflictRetry: () => Promise<void>
}): Promise<void> {
  options.moveError.value = undefined
  const r = options.folderRealm
  try {
    const moveResult = await apiCallWithLoading(() =>
      NotebookController.moveFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        body: options.body,
      })
    )
    throwIfSdkError(moveResult)
    refreshSidebarStructuralListings()
    if (options.body.merge && moveResult.data) {
      await options.router.push({
        name: "folderPage",
        params: {
          notebookId: String(options.destinationNotebookId),
          folderId: String(moveResult.data.id),
        },
      })
      return
    }
    if (options.isCrossNotebookMove) {
      await options.router.push({
        name: "folderPage",
        params: {
          notebookId: String(options.destinationNotebookId),
          folderId: String(r.folder.id),
        },
      })
      return
    }
    await options.fetchFolderPage()
  } catch (e: unknown) {
    const apiError = toOpenApiError(e)
    if (!options.body.merge && isFolderNameConflict(apiError)) {
      const confirmed = await options.confirm(
        `A folder named "${r.folder.name}" already exists at the destination. Merge into it?`
      )
      if (confirmed) {
        await options.onConflictRetry()
        return
      }
    }
    options.moveError.value = apiError.message ?? "Failed to move folder"
  }
}

export async function dissolveFolderOnPage(options: {
  folderRealm: FolderRealm
  merge: boolean
  router: Router
  confirm: FolderAdminConfirm
  dissolveError: Ref<string | undefined>
  onConflictRetry: () => Promise<void>
}): Promise<void> {
  const r = options.folderRealm
  options.dissolveError.value = undefined
  try {
    const dissolveResult = await apiCallWithLoading(() =>
      NotebookController.dissolveFolder({
        path: {
          notebook: r.notebookRealm.notebook.id,
          folder: r.folder.id,
        },
        query: options.merge ? { merge: true } : undefined,
      })
    )
    throwIfSdkError(dissolveResult)
    refreshSidebarStructuralListings()
    await routeAfterFolderDissolve(options.router, r)
  } catch (e: unknown) {
    const apiError = toOpenApiError(e)
    if (!options.merge && isFolderNameConflict(apiError)) {
      const confirmed = await options.confirm(
        "Some subfolders share names with siblings at the destination. Merge them?"
      )
      if (confirmed) {
        await options.onConflictRetry()
        return
      }
    }
    options.dissolveError.value =
      apiError.message ?? "Failed to dissolve folder"
  }
}
