import { waitUntilAppIsNotBusy } from '../pageBase'
import { attachmentPage } from './attachmentPage'
import noteCreationForm from './forms/noteCreationForm'
import {
  openFolderPageAtPath,
  openFolderPageByLabel,
  openFolderPageForOrganize,
  openFolderPageForOrganizeUnderParent,
  openFolderPageUnderOpenParent,
} from './sidebarFolderPageNav'
import {
  sidebarAddFolderButton,
  sidebarAddNoteButton,
} from './sidebarToolbarButtons'
import {
  childTreeitems,
  expandFolder,
  expandFolderPath,
  expectRowLabels,
  folderRowControls,
  folderTreitemByLabel,
  folderTreitemUnderOpenParent,
  revealFolderInSidebar,
  rowsAtPath,
  sidebarActionTimeoutMs,
} from './sidebarTreeItems'

function openSidebarIfCollapsed() {
  cy.document().then((doc) => {
    const show = doc.querySelector('button[aria-label="Show sidebar"]')
    if (show) {
      cy.wrap(show).click()
    }
  })
  cy.get('aside').should('be.visible')
}

function newNoteSidebarButton() {
  waitUntilAppIsNotBusy()
  return sidebarAddNoteButton()
}

export const noteSidebar = () => {
  openSidebarIfCollapsed()

  return {
    expand(label: string) {
      expandFolder(label)
      return this
    },

    expandFolderPath(folderLabels: string[]) {
      expandFolderPath(folderLabels)
      return this
    },

    expectRootRows(expectedLabels: string[]) {
      waitUntilAppIsNotBusy()
      expectRowLabels(rowsAtPath([]), expectedLabels)
    },

    expectRowsUnderFolder(folderLabel: string, expectedLabels: string[]) {
      expandFolder(folderLabel)
      expectRowLabels(
        childTreeitems(folderTreitemByLabel(folderLabel)),
        expectedLabels
      )
    },

    openFile(folderLabels: string[], filename: string) {
      expandFolderPath(folderLabels)
      rowsAtPath(folderLabels)
        .filter(`[aria-label="${filename}"]`)
        .find('.sidebar-attachment-label')
        .click()
      waitUntilAppIsNotBusy()
      return attachmentPage()
    },

    expectOrderedNotes(expectedNotes: Record<string, string>[]) {
      waitUntilAppIsNotBusy()
      const expectedTitles = expectedNotes.map((note) => note['note-title'])
      cy.get('aside [role="treeitem"].sidebar-note-li', {
        timeout: 15000,
      })
        .filter(':visible')
        .should(($els) => {
          const actualNotes = Array.from(
            $els,
            (el) => (el as HTMLElement).getAttribute('aria-label') ?? ''
          )
          expect(actualNotes.length, 'Number of notes should match').to.equal(
            expectedTitles.length
          )
          actualNotes.forEach((actualNote, index) => {
            expect(actualNote, `Note at position ${index + 1}`).to.equal(
              expectedTitles[index]
            )
          })
        })
    },

    addingNoteButton: newNoteSidebarButton,

    addingChildNoteButton: newNoteSidebarButton,

    addingChildNote() {
      sidebarAddNoteButton().click()
      return noteCreationForm
    },

    activateFolderByLabel(folderLabel: string) {
      waitUntilAppIsNotBusy()
      folderTreitemByLabel(folderLabel).click()
      waitUntilAppIsNotBusy()
      return this
    },

    activateFolderUnderOpenParent(parentLabel: string, childLabel: string) {
      waitUntilAppIsNotBusy()
      folderRowControls(folderTreitemUnderOpenParent(parentLabel, childLabel))
        .find('.folder-label-area')
        .click()
      waitUntilAppIsNotBusy()
    },

    openFolderPageByLabel,
    openFolderPageUnderOpenParent,
    openFolderPageAtPath,
    openFolderPageForOrganize,
    openFolderPageForOrganizeUnderParent,

    addingNewNoteFromToolbar() {
      newNoteSidebarButton().click()
      waitUntilAppIsNotBusy()
      return noteCreationForm
    },

    addingNewFolderFromToolbar() {
      waitUntilAppIsNotBusy()
      sidebarAddFolderButton().click()
      return noteCreationForm
    },

    expectSidebarFolderVisible(folderLabel: string) {
      waitUntilAppIsNotBusy()
      folderTreitemByLabel(folderLabel).should(($el) => {
        expect(
          $el.length,
          `Expected sidebar folder "${folderLabel}" to be visible`
        ).to.be.at.least(1)
      })
    },

    expectSidebarFolderAbsent(folderLabel: string) {
      waitUntilAppIsNotBusy()
      cy.get('aside')
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${folderLabel}"]`
        )
        .should('not.exist')
    },

    expectSidebarFolderUnderOpenParent(
      parentFolderLabel: string,
      childFolderLabel: string
    ) {
      waitUntilAppIsNotBusy()
      folderTreitemByLabel(parentFolderLabel)
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${childFolderLabel}"]`
        )
        .should(($el) => {
          expect(
            $el.length,
            `Expected sidebar folder "${childFolderLabel}" under open folder "${parentFolderLabel}"`
          ).to.be.at.least(1)
        })
    },

    expectSidebarNoteUnderOpenFolder(folderLabel: string, noteTitle: string) {
      waitUntilAppIsNotBusy()
      revealFolderInSidebar(folderLabel)
      folderTreitemByLabel(folderLabel)
        .find(`[role="treeitem"].sidebar-note-li[aria-label="${noteTitle}"]`, {
          timeout: sidebarActionTimeoutMs,
        })
        .should(($el) => {
          expect(
            $el.length,
            `Expected note "${noteTitle}" under open folder "${folderLabel}"`
          ).to.be.at.least(1)
        })
    },

    navigateToNote(title: string) {
      waitUntilAppIsNotBusy()
      cy.get('aside')
        .find(`[role="treeitem"].sidebar-note-li[aria-label="${title}"]`, {
          timeout: sidebarActionTimeoutMs,
        })
        .filter(':visible')
        .last()
        .find('a')
        .click()
      waitUntilAppIsNotBusy()
    },

    expectChildrenUnderFolder(
      folderLabel: string,
      children: Record<string, string>[]
    ) {
      waitUntilAppIsNotBusy()
      const expected = children.map(
        (row) => row['note-title'] ?? row.Title ?? ''
      )
      if (expected.length > 0 && expected.some((t) => !t)) {
        throw new Error('each row must include note-title or Title')
      }

      if (expected.length === 0) {
        folderTreitemByLabel(folderLabel)
          .find('[role="treeitem"]')
          .should('not.exist')
        return
      }

      folderTreitemByLabel(folderLabel)
        .find(
          '.folder-children > .sidebar-tree-list > [role="treeitem"].sidebar-note-li',
          { timeout: sidebarActionTimeoutMs }
        )
        .should('have.length', expected.length)
        .then(($items) => {
          const actualTitles = $items
            .toArray()
            .map((el) => (el as HTMLElement).getAttribute('aria-label') ?? '')
          expect([...actualTitles].sort()).to.deep.equal([...expected].sort())
        })
    },
  }
}
