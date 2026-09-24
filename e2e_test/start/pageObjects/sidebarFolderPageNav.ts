import { waitUntilAppIsNotBusy } from '../pageBase'
import folderPage from './folderPage'
import {
  folderRowControls,
  folderTreeitemAtPath,
  folderTreitemByLabel,
  folderTreitemUnderOpenParent,
} from './sidebarTreeItems'

function openFolderPage(
  treeitem: () => Cypress.Chainable<JQuery<HTMLElement>>
) {
  waitUntilAppIsNotBusy()
  folderRowControls(treeitem())
    .find('[data-testid="sidebar-folder-open-page-link"]')
    .click()
  waitUntilAppIsNotBusy()
}

export function openFolderPageByLabel(folderLabel: string) {
  openFolderPage(() => folderTreitemByLabel(folderLabel))
}

export function openFolderPageUnderOpenParent(
  parentLabel: string,
  childLabel: string
) {
  openFolderPage(() => folderTreitemUnderOpenParent(parentLabel, childLabel))
}

export function openFolderPageAtPath(folderLabels: string[]) {
  openFolderPage(() => folderTreeitemAtPath(folderLabels))
}

export function openFolderPageForOrganize(folderLabel: string) {
  openFolderPageByLabel(folderLabel)
  return folderPage().openOrganizeForm()
}

export function openFolderPageForOrganizeUnderParent(
  parentLabel: string,
  childLabel: string
) {
  openFolderPageUnderOpenParent(parentLabel, childLabel)
  return folderPage().openOrganizeForm()
}
