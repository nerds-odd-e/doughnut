import type { Notebook } from "@generated/doughnut-backend-api"
import type { NotebookCatalogEntry } from "@/components/notebook/patchNotebookInCatalogItems"
import { NotebookController } from "@generated/doughnut-backend-api/sdk.gen"
import FolderPage from "@/pages/FolderPage.vue"
import { flushPromises, type VueWrapper } from "@vue/test-utils"
import makeMe from "doughnut-test-fixtures/makeMe"
import helper, { mockSdkService, testFolderStub } from "@tests/helpers"
import usePopups from "@/components/commons/Popups/usePopups"
import { createRouter, createWebHistory, type Router } from "vue-router"
import routes from "@/routes/routes"
import { vi } from "vitest"

export const folderNameConflictMessage =
  "A folder with this name already exists here."

export const softDeletedTitleConflictMessage =
  "A note with this title already exists here but was deleted. Restore the deleted note (Undo delete), or choose another title."

export function createFolderPageRouter() {
  return createRouter({ history: createWebHistory(), routes })
}

export function stubFolderPageListingMocks(
  catalogItems: NotebookCatalogEntry[]
) {
  mockSdkService(NotebookController, "listNotebookFolderIndex", [])
  mockSdkService(NotebookController, "listNotebookFolderListing", {
    folders: [],
  })
  mockSdkService(NotebookController, "myNotebooks", {
    notebooks: [],
    catalogItems,
    subscriptions: [],
  })
}

export type MountFolderPageOptions = {
  fetchFolderPage?: ReturnType<typeof vi.fn>
  extraNotebooks?: Notebook[]
}

export function mountFolderPage(
  router: Router,
  folderId: number,
  folderName: string,
  options: MountFolderPageOptions = {}
) {
  const fetchFolderPage =
    options.fetchFolderPage ?? vi.fn().mockResolvedValue(undefined)
  const extraNotebooks = options.extraNotebooks ?? []
  const folderRealm = makeMe.aFolderRealm.folder(folderId, folderName).please()
  stubFolderPageListingMocks(
    makeMe.notebookCatalog
      .notebooks(folderRealm.notebookRealm.notebook, ...extraNotebooks)
      .please()
  )
  const wrapper = helper
    .component(FolderPage)
    .withCleanStorage()
    .withRouter(router)
    .withProps({ folderRealm, fetchFolderPage })
    .mount()
  return { wrapper, folderRealm }
}

export async function mountFolderPageReady(
  router: Router,
  folderId: number,
  folderName: string,
  options: MountFolderPageOptions = {}
) {
  const mounted = mountFolderPage(router, folderId, folderName, options)
  await flushPromises()
  return mounted
}

export async function mountCrossNotebookRootMovePage(
  router: Router,
  folderId: number,
  folderName: string
) {
  const destinationNotebook = makeMe.aNotebook.please()
  const mounted = await mountFolderPageReady(router, folderId, folderName, {
    extraNotebooks: [destinationNotebook],
  })
  return { ...mounted, destinationNotebook }
}

export async function mountCrossNotebookFolderMovePage(
  router: Router,
  folderId: number,
  folderName: string,
  destParent = testFolderStub(50, "DestParent")
) {
  const destinationNotebook = makeMe.aNotebook.please()
  const mounted = await mountFolderPageReady(router, folderId, folderName, {
    extraNotebooks: [destinationNotebook],
  })
  mockSdkService(NotebookController, "listNotebookFolderListing", {
    folders: [destParent],
  })
  return { ...mounted, destinationNotebook, destParent }
}

export function stubRouterPush(router: Router) {
  return vi.spyOn(router, "push").mockResolvedValue(undefined as never)
}

export async function selectCrossNotebookDestination(
  wrapper: VueWrapper,
  destinationNotebookId: number,
  destParentId: number
) {
  await selectDestinationNotebook(wrapper, destinationNotebookId)
  await flushPromises()
  await selectDestinationParentFolder(wrapper, destParentId)
}

export async function submitMoveForm(wrapper: VueWrapper) {
  await wrapper
    .find('[data-testid="folder-move-dialog"] form')
    .trigger("submit")
  await flushPromises()
}

export async function setRenameName(wrapper: VueWrapper, name: string) {
  const nameInput = wrapper.find('[data-test="folder-name"]')
    .element as HTMLElement
  nameInput.innerText = name
  nameInput.dispatchEvent(new Event("input", { bubbles: true }))
  await flushPromises()
}

export async function submitRenameForm(wrapper: VueWrapper) {
  const renameForm = wrapper
    .find('[data-testid="folder-rename-submit"]')
    .element.closest("form")
  if (renameForm == null) {
    throw new Error("Rename form not found")
  }
  renameForm.dispatchEvent(
    new Event("submit", { bubbles: true, cancelable: true })
  )
  await flushPromises()
}

export async function selectDestinationNotebook(
  wrapper: VueWrapper,
  notebookId: number
) {
  await wrapper
    .get('[data-testid="folder-move-notebook-select"]')
    .setValue(String(notebookId))
  await flushPromises()
}

export async function selectDestinationParentFolder(
  wrapper: VueWrapper,
  folderId: number
) {
  await wrapper
    .get('[data-testid="folder-move-parent-select"]')
    .setValue(String(folderId))
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
