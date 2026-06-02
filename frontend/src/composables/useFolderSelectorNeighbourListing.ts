import type { Folder } from "@generated/doughnut-backend-api"
import { apiCallWithLoading } from "@/managedApi/clientSetup"
import { requestNotebookFolderListing } from "@/utils/notebookFolderListingRequest"
import { ref, type Ref } from "vue"

export function useFolderSelectorNeighbourListing(
  notebookId: Ref<number>,
  parentFolderId: Ref<number | null>
) {
  const neighbourFolders = ref<Folder[]>([])
  const loadError = ref<string | undefined>(undefined)

  async function loadNeighbourFolders() {
    try {
      loadError.value = undefined
      const { data: listing, error } = await apiCallWithLoading(() =>
        requestNotebookFolderListing(notebookId.value, parentFolderId.value)
      )
      if (error || !listing)
        throw new Error("Failed to load neighbouring folders")
      neighbourFolders.value = listing.folders ?? []
    } catch {
      loadError.value = "Failed to load neighbouring folders"
    }
  }

  return { neighbourFolders, loadError, loadNeighbourFolders }
}
