import { pageIsNotLoading } from '../pageBase'
import noteCreationForm from './noteForms/noteCreationForm'
import { assumeSidebarFolderOrganizeDialog } from './sidebarFolderOrganizeDialog'

const sidebarActionTimeoutMs = 20000

type AsideSegmentKind = 'folder' | 'note'

/** Detect whether `label` is a folder or note treeitem in the sidebar DOM. */
function asideSegmentKind(
  aside: HTMLElement,
  label: string
): AsideSegmentKind | null {
  for (const el of aside.querySelectorAll<HTMLElement>('[role="treeitem"]')) {
    if (el.getAttribute('aria-label') !== label) continue
    if (el.classList.contains('sidebar-folder-li')) return 'folder'
    if (el.classList.contains('sidebar-note-li')) return 'note'
  }
  return null
}

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

function expandFolderIfCollapsed(label: string) {
  pageIsNotLoading()
  folderTreitemByLabel(label).then(($li) => {
    if ($li.attr('aria-expanded') !== 'true') {
      cy.wrap($li).find('.folder-row .chevron-btn').first().click()
    }
  })
  pageIsNotLoading()
  folderTreitemByLabel(label)
    .should('have.attr', 'aria-expanded', 'true')
    .find('[role="treeitem"]', { timeout: sidebarActionTimeoutMs })
    .should('have.length.at.least', 1)
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
    expand: (label: string) => expandFolderIfCollapsed(label),

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
      folderTreitemByLabel(folderLabel).find('.sidebar-folder-label').click()
      pageIsNotLoading()
    },

    openFolderOrganizeDialog() {
      pageIsNotLoading()
      cy.get('aside').findByRole('button', { name: 'Folder…' }).click()
      return assumeSidebarFolderOrganizeDialog()
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

    expectSidebarFolderUnderParent(
      parentFolderLabel: string,
      childFolderLabel: string
    ) {
      pageIsNotLoading()
      expandFolderIfCollapsed(parentFolderLabel)
      folderTreitemByLabel(parentFolderLabel)
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${childFolderLabel}"]`
        )
        .should('have.length.at.least', 1)
    },

    expectSidebarFolderNotUnderParent(
      parentFolderLabel: string,
      childFolderLabel: string
    ) {
      pageIsNotLoading()
      expandFolderIfCollapsed(parentFolderLabel)
      folderTreitemByLabel(parentFolderLabel)
        .find(
          `[role="treeitem"].sidebar-folder-li[aria-label="${childFolderLabel}"]`
        )
        .should('not.exist')
    },

    /**
     * After the notebook card: expand a folder row for this segment, or open the note row.
     * Waits for sidebar hydration so a folder label is not mistaken for a missing note title.
     */
    navigateStructuralIntermediate(segment: string) {
      pageIsNotLoading()
      const label = segment.trim()

      cy.get('aside', { timeout: sidebarActionTimeoutMs }).should(($aside) => {
        expect(
          asideSegmentKind($aside.get(0)!, label),
          `sidebar should load "${label}" as folder or note row`
        ).to.not.be.null
      })

      cy.get('aside').then(($aside) => {
        if (asideSegmentKind($aside.get(0)!, label) === 'folder') {
          expandFolderIfCollapsed(segment)
        } else {
          cy.get('aside')
            .find(`[role="treeitem"].sidebar-note-li[aria-label="${label}"]`, {
              timeout: sidebarActionTimeoutMs,
            })
            .filter(':visible')
            .last()
            .find('a')
            .click()
          pageIsNotLoading()
        }
      })
    },

    expandStructuralIntermediateFolderOnly(segment: string) {
      expandFolderIfCollapsed(segment)
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
  addingNoteButton() {
    return noteSidebar().addingNoteButton()
  },
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
