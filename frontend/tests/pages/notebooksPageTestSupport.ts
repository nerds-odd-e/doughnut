import type {
  Notebook,
  SubscriptionForNotebooksListing,
} from "@generated/donut-backend-api"
import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
import helper, { mockSdkService } from "@tests/helpers"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { Router } from "vue-router"

export function clearNotebooksPageStorage() {
  localStorage.removeItem("doughnut.notebooksPage.layout")
  sessionStorage.removeItem(PEER_SORT_STORAGE_KEY)
  localStorage.removeItem(PEER_SORT_STORAGE_KEY)
}

export function catalogHeadingTexts(wrapper: VueWrapper) {
  return wrapper.findAll("h3, h5").map((w) => w.text())
}

export function mockMyNotebooks(options: {
  notebooks: Array<{ notebook: Notebook; hasAttachedBook?: boolean }>
  catalogItems?: NotebookCatalogEntry[]
  subscriptions?: SubscriptionForNotebooksListing[]
}) {
  const catalogItems =
    options.catalogItems ??
    makeMe.notebookCatalog
      .notebooks(
        ...options.notebooks.map(({ notebook, hasAttachedBook }) =>
          hasAttachedBook === undefined
            ? notebook
            : { ...notebook, hasAttachedBook }
        )
      )
      .please()
  return mockSdkService(NotebookController, "myNotebooks", {
    notebooks: options.notebooks,
    catalogItems,
    subscriptions: options.subscriptions ?? [],
  })
}

export async function mountNotebooksPage(router?: Router): Promise<VueWrapper> {
  const wrapper = helper
    .component(NotebooksPage)
    .withCurrentUser(makeMe.aUser.please())
    .withRouter(router)
    .mount()
  await flushPromises()
  return wrapper
}

export async function emitNotebookUpdated(
  wrapper: VueWrapper,
  notebook: Notebook
) {
  await wrapper
    .findComponent(NotebooksPageView)
    .vm.$emit("notebook-updated", notebook)
  await flushPromises()
}
