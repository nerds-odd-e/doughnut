import { pageIsNotLoading } from '../pageBase'
import folderPage from './folderPage'
import { assumeSidebarFolderOrganizeForm } from './sidebarFolderOrganizeForm'

const sidebarActionTimeoutMs = 20000

function folderTreitemByLabel(folderLabel: string) {
  return cy
    .get('aside')
    .find(`[role="treeitem"].sidebar-folder-li[aria-label="${folderLabel}"]`, {
      timeout: sidebarActionTimeoutMs,
    })
    .filter(':visible')
    .last()
}

function folderRowControls(treeitem: Cypress.Chainable<JQuery<HTMLElement>>) {
  return treeitem.children('.folder-row')
}

function openFolderPageLink(treeitem: Cypress.Chainable<JQuery<HTMLElement>>) {
  pageIsNotLoading()
  folderRowControls(treeitem)
    .find('[data-testid="sidebar-folder-open-page-link"]')
    .click()
  pageIsNotLoading()
  folderPage().openSettingsTab()
  return assumeSidebarFolderOrganizeForm()
}

export function openFolderPageForOrganize(folderLabel: string) {
  return openFolderPageLink(folderTreitemByLabel(folderLabel))
}

export function openFolderPageForOrganizeUnderParent(
  parentLabel: string,
  childLabel: string
) {
  const childFolder = folderTreitemByLabel(parentLabel).find(
    `[role="treeitem"].sidebar-folder-li[aria-label="${childLabel}"]`,
    { timeout: sidebarActionTimeoutMs }
  )
  return openFolderPageLink(childFolder.filter(':visible').last())
}
