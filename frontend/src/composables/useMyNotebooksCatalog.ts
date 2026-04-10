import { ref } from "vue"
import type { Notebook, Subscription } from "@generated/doughnut-backend-api"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import {
  patchNotebookInCatalogItems,
  type NotebookCatalogEntry,
} from "@/components/notebook/patchNotebookInCatalogItems"

export function useMyNotebooksCatalog() {
  const subscriptions = ref<Subscription[] | undefined>(undefined)
  const catalogItems = ref<NotebookCatalogEntry[] | undefined>(undefined)

  const fetchData = async () => {
    const { data: result, error } = await NotebookController.myNotebooks({})
    if (!error) {
      catalogItems.value = result!.catalogItems
      subscriptions.value = result!.subscriptions
    }
  }

  const handleNotebookUpdated = (updatedNotebook: Notebook) => {
    if (catalogItems.value) {
      catalogItems.value = patchNotebookInCatalogItems(
        catalogItems.value,
        updatedNotebook
      )
    }
  }

  return {
    catalogItems,
    subscriptions,
    fetchData,
    handleNotebookUpdated,
  }
}
