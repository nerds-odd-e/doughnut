import type { Folder } from "@generated/donut-backend-api"
import { loadFolderListing } from "@/utils/notebookFolderListingRequest"
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
      const listing = await loadFolderListing(
        notebookId.value,
        parentFolderId.value
      )
      neighbourFolders.value = listing.folders ?? []
    } catch {
      loadError.value = "Failed to load neighbouring folders"
    }
  }

  return { neighbourFolders, loadError, loadNeighbourFolders }
}
