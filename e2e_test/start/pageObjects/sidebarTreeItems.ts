import { waitUntilAppIsNotBusy } from '../pageBase'

export const sidebarActionTimeoutMs = 20000

export function noteTree() {
  return cy.get('aside').find('[role="tree"][aria-label="Note tree"]', {
    timeout: sidebarActionTimeoutMs,
  })
}

/** Direct child rows (folders, notes, files) of an expanded folder treeitem. */
export function childTreeitems(
  parentTreeitem: Cypress.Chainable<JQuery<HTMLElement>>
) {
  return parentTreeitem
    .children('.folder-children')
    .children('[role="group"].sidebar-tree-list')
    .children('[role="treeitem"]')
}

function visibleFolderLabelled(
  treeitems: Cypress.Chainable<JQuery<HTMLElement>>,
  label: string
) {
  return treeitems
    .filter('.sidebar-folder-li')
    .filter((_index, element) => element.getAttribute('aria-label') === label)
    .filter(':visible')
    .last()
}

/** Deepest visible folder treeitem matching `folderLabel`. */
export function folderTreitemByLabel(folderLabel: string) {
  return cy
    .get('aside')
    .find(`[role="treeitem"].sidebar-folder-li[aria-label="${folderLabel}"]`, {
      timeout: sidebarActionTimeoutMs,
    })
    .filter(':visible')
    .last()
}

/** This folder row only (not nested subfolder rows). */
export function folderRowControls(
  treeitem: Cypress.Chainable<JQuery<HTMLElement>>
) {
  return treeitem.children('.folder-row')
}

/** Deepest visible child folder treeitem under an expanded parent. */
export function folderTreitemUnderOpenParent(
  parentLabel: string,
  childLabel: string
) {
  return folderTreitemByLabel(parentLabel)
    .find(`[role="treeitem"].sidebar-folder-li[aria-label="${childLabel}"]`, {
      timeout: sidebarActionTimeoutMs,
    })
    .filter(':visible')
    .last()
}

export function folderTreeitemAtPath(folderLabels: string[]) {
  const [rootLabel, ...childLabels] = folderLabels
  if (rootLabel == null) throw new Error('folder path must not be empty')

  let treeitem = visibleFolderLabelled(
    noteTree().children('[role="treeitem"]'),
    rootLabel
  )
  for (const childLabel of childLabels) {
    treeitem = visibleFolderLabelled(childTreeitems(treeitem), childLabel)
  }
  return treeitem
}

export function expectRowLabels(
  rows: Cypress.Chainable<JQuery<HTMLElement>>,
  expectedLabels: string[]
) {
  rows.should(($items) => {
    expect(
      $items.toArray().map((el) => el.getAttribute('aria-label'))
    ).to.deep.equal(expectedLabels)
  })
}

export function expandFolder(label: string) {
  waitUntilAppIsNotBusy()
  revealFolderInSidebar(label)
  folderTreitemByLabel(label)
    .find('[role="treeitem"]', { timeout: sidebarActionTimeoutMs })
    .should('have.length.at.least', 1)
}

function expandFolderTreeitem(
  treeitem: () => Cypress.Chainable<JQuery<HTMLElement>>
) {
  treeitem().then(($el) => {
    if (($el.attr('aria-expanded') ?? 'false') === 'false') {
      $el.find('.folder-row .chevron-btn').first()[0].click()
    }
  })
  waitUntilAppIsNotBusy()
  treeitem().should('have.attr', 'aria-expanded', 'true')
}

/** Expand a folder row so note children are in the DOM (no subfolder requirement). */
export function revealFolderInSidebar(label: string) {
  expandFolderTreeitem(() => folderTreitemByLabel(label))
}

export function expandFolderPath(folderLabels: string[]) {
  folderLabels.forEach((_label, index) => {
    expandFolderTreeitem(() =>
      folderTreeitemAtPath(folderLabels.slice(0, index + 1))
    )
  })
}
