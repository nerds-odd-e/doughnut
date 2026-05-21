import { pageIsNotLoading } from '../pageBase'
import noteCreationForm from './forms/noteCreationForm'
import { assumeSidebarFolderOrganizeForm } from './sidebarFolderOrganizeForm'

const sidebarActionTimeoutMs = 20000

/** Deepest visible folder treeitem matching `folderLabel`. */
function folderTreitemByLabel(folderLabel: string) {
  return cy
    .get('aside')
    .find(`[role="treeitem"].sidebar-folder-li[aria-label="${folderLabel}"]`, {
      timeout: sidebarActionTimeoutMs,
    })
    .filter(':visible')
    .last()
}

function expandFolder(label: string) {
  pageIsNotLoading()
  revealFolderInSidebar(label)
  folderTreitemByLabel(label)
    .find('[role="treeitem"]', { timeout: sidebarActionTimeoutMs })
    .should('have.length.at.least', 1)
  return
}

/** Expand a folder row so note children are in the DOM (no subfolder requirement). */
function revealFolderInSidebar(label: string) {
  folderTreitemByLabel(label).then(($el) => {
    if (($el.attr('aria-expanded') ?? 'false') === 'false') {
      cy.wrap($el).find('.folder-row .chevron-btn').first().click()
    }
  })
  folderTreitemByLabel(label).should('have.attr', 'aria-expanded', 'true')
}

function openSidebarIfCollapsed() {
  cy.document().then((doc) => {
    const show = doc.querySelector('button[aria-label="Show sidebar"]')
    if (show) {
      cy.wrap(show).click()
    }
  })
  cy.get('aside').should('be.visible')
}

const sidebarAddNoteButton = (buttonName?: string) => {
  const getButton = () =>
    cy.get('aside').findByRole('button', {
      name: buttonName ?? 'New note',
    })
  return {
    click: () => {
      getButton().click({ force: true })
      return noteCreationForm
    },
    shouldNotExist: () => getButton().should('not.exist'),
  }
}

function newNoteSidebarButton() {
  pageIsNotLoading()
  return sidebarAddNoteButton('New note')
}

export const noteSidebar = () => {
  openSidebarIfCollapsed()

  return {
    expand(label: string) {
      expandFolder(label)
      return this
    },

    expectOrderedNotes(expectedNotes: Record<string, string>[]) {
      pageIsNotLoading()
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
      sidebarAddNoteButton('New note').click()
      return noteCreationForm
    },

    activateFolderByLabel(folderLabel: string) {
      pageIsNotLoading()
      folderTreitemByLabel(folderLabel).click()
      pageIsNotLoading()
      return this
    },

    activateFolderUnderOpenParent(parentLabel: string, childLabel: string) {
      pageIsNotLoading()
      folderTreitemByLabel(parentLabel)
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${childLabel}"]`,
          { timeout: sidebarActionTimeoutMs }
        )
        .filter(':visible')
        .last()
        .findByText(childLabel)
        .click()
      pageIsNotLoading()
    },

    openFolderPageByLabel(folderLabel: string) {
      pageIsNotLoading()
      folderTreitemByLabel(folderLabel)
        .find('[data-testid="sidebar-folder-open-page-link"]')
        .click()
      pageIsNotLoading()
    },

    openFolderPageForOrganize(folderLabel: string) {
      pageIsNotLoading()
      folderTreitemByLabel(folderLabel)
        .find('[data-testid="sidebar-folder-open-page-link"]')
        .click()
      pageIsNotLoading()
      return assumeSidebarFolderOrganizeForm()
    },

    openFolderPageForOrganizeUnderParent(
      parentLabel: string,
      childLabel: string
    ) {
      pageIsNotLoading()
      folderTreitemByLabel(parentLabel)
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${childLabel}"]`,
          { timeout: sidebarActionTimeoutMs }
        )
        .filter(':visible')
        .last()
        .find('[data-testid="sidebar-folder-open-page-link"]')
        .click()
      pageIsNotLoading()
      return assumeSidebarFolderOrganizeForm()
    },

    addingNewNoteFromToolbar() {
      newNoteSidebarButton().click()
      return noteCreationForm
    },

    addingNewFolderFromToolbar() {
      pageIsNotLoading()
      sidebarAddNoteButton('New folder').click()
      return noteCreationForm
    },

    expectSidebarFolderVisible(folderLabel: string) {
      pageIsNotLoading()
      folderTreitemByLabel(folderLabel).should('exist')
    },

    expectSidebarFolderUnderOpenParent(
      parentFolderLabel: string,
      childFolderLabel: string
    ) {
      pageIsNotLoading()
      folderTreitemByLabel(parentFolderLabel)
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${childFolderLabel}"]`
        )
        .should('have.length.at.least', 1)
    },

    expectSidebarNoteUnderOpenFolder(folderLabel: string, noteTitle: string) {
      pageIsNotLoading()
      revealFolderInSidebar(folderLabel)
      folderTreitemByLabel(folderLabel)
        .find(`[role="treeitem"].sidebar-note-li[aria-label="${noteTitle}"]`, {
          timeout: sidebarActionTimeoutMs,
        })
        .should('have.length.at.least', 1)
    },

    navigateToNote(title: string) {
      pageIsNotLoading()
      cy.get('aside')
        .find(`[role="treeitem"].sidebar-note-li[aria-label="${title}"]`, {
          timeout: sidebarActionTimeoutMs,
        })
        .filter(':visible')
        .last()
        .find('a')
        .click()
      pageIsNotLoading()
    },

    expectChildrenUnderFolder(
      folderLabel: string,
      children: Record<string, string>[]
    ) {
      pageIsNotLoading()
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

export const sidebarChildNotePageMethods = () => ({
  addingChildNoteButton() {
    return noteSidebar().addingChildNoteButton()
  },
  addingChildNote() {
    return noteSidebar().addingChildNote()
  },
  addingNewNoteFromToolbar() {
    return noteSidebar().addingNewNoteFromToolbar()
  },
  addingNewFolderFromToolbar() {
    return noteSidebar().addingNewFolderFromToolbar()
  },
})
