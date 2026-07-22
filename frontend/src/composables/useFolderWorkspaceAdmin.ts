import type { Folder, FolderRealm } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import { computed, onMounted, ref, watch, type Ref } from "vue"
import { useRouter } from "vue-router"
import { notebooksFromCatalogItems } from "@/components/notebook/notebooksFromCatalogItems"
import { sortNotebookCatalogAlphabetically } from "@/components/notebook/sortNotebookCatalogAlphabetically"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import usePopups from "@/components/commons/Popups/usePopups"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { toOpenApiError } from "@/managedApi/openApiError"
import {
  buildFolderMoveBody,
  dissolveFolderOnPage,
  dissolveParentLabelFromChain,
  moveFolderOnPage,
  renameFolderOnPage,
} from "@/composables/folderAdminMutations"

/**
 * Move / rename / dissolve admin state for the folder Settings tab.
 */
export function useFolderWorkspaceAdmin(
  folderRealm: Ref<FolderRealm>,
  fetchFolderPage: () => Promise<void>
) {
  const router = useRouter()
  const { popups } = usePopups()

  const processing = ref(false)
  const moveError = ref<string | undefined>(undefined)
  const dissolveError = ref<string | undefined>(undefined)
  const renameError = ref<string | undefined>(undefined)
  const selectedParentFolder = ref<Folder | null>(null)
  const renameName = ref("")
  const destinationCatalogItems = ref<NotebookCatalogEntry[] | undefined>(
    undefined
  )
  const notebooksLoading = ref(true)
  const notebooksLoadError = ref<string | undefined>(undefined)
  const selectedDestinationNotebookId = ref<number | undefined>(undefined)

  const sourceNotebookId = computed(
    () => folderRealm.value.notebookRealm.notebook.id
  )

  const destinationNotebooks = computed(() => {
    if (destinationCatalogItems.value == null) return []
    return notebooksFromCatalogItems(
      sortNotebookCatalogAlphabetically(destinationCatalogItems.value)
    )
  })

  const isCrossNotebookMove = computed(() => {
    const sourceId = sourceNotebookId.value
    const destinationId = selectedDestinationNotebookId.value
    return (
      sourceId != null && destinationId != null && destinationId !== sourceId
    )
  })

  const folderPickerNotebookId = computed(() => {
    if (
      isCrossNotebookMove.value &&
      selectedDestinationNotebookId.value != null
    ) {
      return selectedDestinationNotebookId.value
    }
    return folderRealm.value.notebookRealm.notebook.id
  })

  const folderPickerContextFolder = computed((): Folder | null => {
    if (isCrossNotebookMove.value) return null
    return folderRealm.value.folder
  })

  const folderPickerAncestorFolders = computed((): Folder[] => {
    if (isCrossNotebookMove.value) return []
    return folderRealm.value.ancestorFolders ?? []
  })

  watch(
    sourceNotebookId,
    (notebookId) => {
      if (notebookId == null) return
      if (selectedDestinationNotebookId.value == null) {
        selectedDestinationNotebookId.value = notebookId
      }
    },
    { immediate: true }
  )

  watch(selectedDestinationNotebookId, (destinationId) => {
    const sourceId = sourceNotebookId.value
    if (
      destinationId != null &&
      sourceId != null &&
      destinationId !== sourceId
    ) {
      selectedParentFolder.value = null
    }
  })

  onMounted(async () => {
    notebooksLoadError.value = undefined
    try {
      const { data, error } = await apiCallWithLoading(() =>
        NotebookController.myNotebooks({})
      )
      if (error || !data) throw new Error("Failed to load notebooks")
      destinationCatalogItems.value = data.catalogItems
      if (
        selectedDestinationNotebookId.value == null &&
        sourceNotebookId.value != null
      ) {
        selectedDestinationNotebookId.value = sourceNotebookId.value
      }
    } catch (e: unknown) {
      notebooksLoadError.value =
        toOpenApiError(e).message ?? "Failed to load notebooks"
    } finally {
      notebooksLoading.value = false
    }
  })

  watch(
    () => folderRealm.value.folder.id,
    (id) => {
      if (id == null) return
      renameName.value = folderRealm.value.folder.name
      renameError.value = undefined
    },
    { immediate: true }
  )

  const renameSubmitDisabled = computed(
    () =>
      processing.value ||
      renameName.value.trim().length === 0 ||
      renameName.value.trim() === folderRealm.value.folder.name
  )

  const dissolveParentLabel = computed(() =>
    dissolveParentLabelFromChain(
      folderRealm.value.folder.id,
      folderRealm.value.ancestorFolders ?? []
    )
  )

  const submitRename = async () => {
    if (processing.value || renameSubmitDisabled.value) return
    processing.value = true
    try {
      await renameFolderOnPage({
        folderRealm: folderRealm.value,
        newName: renameName.value.trim(),
        fetchFolderPage,
        renameError,
      })
    } catch (e: unknown) {
      renameError.value = toOpenApiError(e).message ?? "Failed to rename folder"
    } finally {
      processing.value = false
    }
  }

  const submitMove = async (merge = false) => {
    if (processing.value) return
    processing.value = true
    const r = folderRealm.value
    const destinationNotebookId =
      selectedDestinationNotebookId.value ?? r.notebookRealm.notebook.id
    try {
      await moveFolderOnPage({
        folderRealm: r,
        body: buildFolderMoveBody({
          isCrossNotebookMove: isCrossNotebookMove.value,
          destinationNotebookId: selectedDestinationNotebookId.value,
          selectedParentFolder: selectedParentFolder.value,
          merge,
        }),
        destinationNotebookId,
        isCrossNotebookMove: isCrossNotebookMove.value,
        fetchFolderPage,
        router,
        confirm: (msg) => popups.confirm(msg),
        moveError,
        onConflictRetry: async () => {
          processing.value = false
          await submitMove(true)
        },
      })
    } finally {
      processing.value = false
    }
  }

  const dissolve = async (merge = false) => {
    const r = folderRealm.value
    if (processing.value) return
    if (!merge) {
      const ok = await popups.confirm(
        `Dissolve folder "${r.folder.name}"? Notes and subfolders will be kept.`
      )
      if (!ok) return
    }
    processing.value = true
    try {
      await dissolveFolderOnPage({
        folderRealm: r,
        merge,
        router,
        confirm: (msg) => popups.confirm(msg),
        dissolveError,
        onConflictRetry: async () => {
          processing.value = false
          await dissolve(true)
        },
      })
    } finally {
      processing.value = false
    }
  }

  return {
    processing,
    moveError,
    dissolveError,
    renameError,
    selectedParentFolder,
    renameName,
    destinationNotebooks,
    notebooksLoading,
    notebooksLoadError,
    selectedDestinationNotebookId,
    folderPickerNotebookId,
    folderPickerContextFolder,
    folderPickerAncestorFolders,
    renameSubmitDisabled,
    dissolveParentLabel,
    submitRename,
    submitMove,
    dissolve,
  }
}
