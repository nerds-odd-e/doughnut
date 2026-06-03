import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import FolderPage from "@/pages/FolderPage.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { createRouter, createWebHistory, type Router } from "vue-router"
import routes from "@/routes/routes"
import { vi } from "vitest"

export function createFolderPageRouter() {
  return createRouter({ history: createWebHistory(), routes })
}

export function stubFolderPageListingMocks() {
  mockSdkService(NotebookController, "listNotebookFolderIndex", [])
  mockSdkService(NotebookController, "listNotebookFolderListing", {
    folders: [],
  })
}

export function mountFolderPage(
  router: Router,
  folderId: number,
  folderName: string,
  fetchFolderPage = vi.fn().mockResolvedValue(undefined)
) {
  const folderRealm = makeMe.aFolderRealm.folder(folderId, folderName).please()
  const wrapper = helper
    .component(FolderPage)
    .withCleanStorage()
    .withRouter(router)
    .withProps({ folderRealm, fetchFolderPage })
    .mount()
  return { wrapper, folderRealm }
}

export async function submitMoveForm(wrapper: VueWrapper) {
  await wrapper
    .find('[data-testid="folder-move-dialog"] form')
    .trigger("submit")
  await flushPromises()
}

export async function dissolveWithInitialConfirm(wrapper: VueWrapper) {
  await wrapper.find('[data-testid="folder-dissolve-button"]').trigger("click")
  await flushPromises()
  usePopups().popups.done(true)
  await flushPromises()
}

export function resolveTopConfirm(confirmed: boolean) {
  usePopups().popups.done(confirmed)
}
