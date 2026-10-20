import type {
  Notebook,
  SubscriptionForNotebooksListing,
} from "@generated/donut-backend-api"
import { NotebookController } from "@generated/donut-backend-api/sdk.gen"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import NotebooksPage from "@/pages/NotebooksPage.vue"
import NotebooksPageView from "@/pages/NotebooksPageView.vue"
import { PEER_SORT_STORAGE_KEY } from "@/composables/usePeerSort"
import helper, { mockSdkService, wrapSdkResponse } from "@tests/helpers"
import { vi } from "vitest"
import makeMe from "donut-test-fixtures/makeMe"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import type { Router } from "vue-router"

export function clearNotebooksPageStorage() {
  localStorage.removeItem("donut.notebooksPage.layout")
  sessionStorage.removeItem(PEER_SORT_STORAGE_KEY)
  localStorage.removeItem(PEER_SORT_STORAGE_KEY)
}

export function catalogHeadingTexts(wrapper: VueWrapper) {
  return wrapper.findAll("h3, h5").map((w) => w.text())
}

type MyNotebooksOptions = {
  notebooks: Array<{ notebook: Notebook; hasAttachedBook?: boolean }>
  catalogItems?: NotebookCatalogEntry[]
  subscriptions?: SubscriptionForNotebooksListing[]
}

function myNotebooksResponse(options: MyNotebooksOptions) {
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
  return {
    notebooks: options.notebooks,
    catalogItems,
    subscriptions: options.subscriptions ?? [],
  }
}

export function mockMyNotebooks(options: MyNotebooksOptions) {
  return mockSdkService(
    NotebookController,
    "myNotebooks",
    myNotebooksResponse(options)
  )
}

/** Keeps the catalog loading until the returned function finishes it. */
export function mockMyNotebooksLoading(options: MyNotebooksOptions) {
  let finishLoading!: () => void
  vi.spyOn(NotebookController, "myNotebooks").mockReturnValue(
    new Promise((resolve) => {
      finishLoading = () =>
        resolve(wrapSdkResponse(myNotebooksResponse(options)))
    }) as never
  )
  return () => finishLoading()
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
