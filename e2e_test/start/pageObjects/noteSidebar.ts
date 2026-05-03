import { pageIsNotLoading } from '../pageBase'
import noteCreationForm from './noteForms/noteCreationForm'

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

function openSidebarIfCollapsed() {
  cy.findByRole('button', { name: 'toggle sidebar' }).then(($button) => {
    if (!$button.hasClass('sidebar-expanded')) {
      cy.wrap($button).click()
    }
  })
  cy.get('aside').should('be.visible')
}

/** Exact label on `.sidebar-folder-label`, visible, deepest match (mirrors structural tree). */
function folderRowByExactLabel(folderLabel: string) {
  return cy
    .get('.sidebar-folder-label')
    .filter((_, el) => el.textContent?.trim() === folderLabel.trim())
    .filter(':visible')
    .last()
    .should('be.visible')
    .closest('li')
}

function expandFolderRowIfCollapsed(segment: string) {
  pageIsNotLoading()
  const label = segment.trim()
  cy.get('aside').within(() => {
    folderRowByExactLabel(label).then(($li) => {
      if ($li.attr('data-sidebar-folder-expanded') !== 'true') {
        cy.wrap($li).within(() => {
          cy.findByTitle('expand children').click()
        })
      }
    })
  })
  pageIsNotLoading()
  cy.get('aside').within(() => {
    folderRowByExactLabel(label).within(() => {
      // After expand, scope may list only nested folder rows (no `.title-text`) when the
      // folder-named note was deleted but child folders/notes remain.
      cy.get('ul.daisy-list-group li, .title-text', { timeout: 20000 }).should(
        'have.length.at.least',
        1
      )
    })
  })
}

export const noteSidebar = () => {
  openSidebarIfCollapsed()

  return {
    expand: (noteTopology: string) => expandFolderRowIfCollapsed(noteTopology),

    siblingOrder: (higher: string, lower: string) => {
      cy.get('aside').within(() => {
        cy.contains(higher).parent().parent().nextAll().contains(lower)
      })
    },

    expectOrderedNotes(expectedNotes: Record<string, string>[]) {
      pageIsNotLoading()
      const expectedNoteTopics = expectedNotes.map((note) => note['note-title'])

      cy.get('aside ul li .title-text', { timeout: 15000 }).should(($els) => {
        const actualNotes = Array.from($els, (el) => el.innerText)

        expect(actualNotes.length, 'Number of notes should match').to.equal(
          expectedNoteTopics.length
        )

        actualNotes.forEach((actualNote, index) => {
          expect(actualNote, `Note at position ${index + 1}`).to.equal(
            expectedNoteTopics[index]
          )
        })
      })
    },

    addingNoteButton() {
      pageIsNotLoading()
      return sidebarAddNoteButton('New note')
    },

    addingChildNoteButton() {
      pageIsNotLoading()
      return sidebarAddNoteButton('New note')
    },

    addingChildNote() {
      sidebarAddNoteButton('New note').click()
      return noteCreationForm
    },

    activateFolderByLabel(folderLabel: string) {
      pageIsNotLoading()
      cy.get('aside').within(() => {
        folderRowByExactLabel(folderLabel)
          .find('.sidebar-folder-label')
          .should('be.visible')
          .click()
      })
      pageIsNotLoading()
    },

    addingNewNoteFromToolbar() {
      pageIsNotLoading()
      sidebarAddNoteButton('New note').click()
      return noteCreationForm
    },

    addingNewFolderFromToolbar() {
      pageIsNotLoading()
      sidebarAddNoteButton('New folder').click()
      return noteCreationForm
    },

    expectSidebarFolderVisible(folderLabel: string) {
      pageIsNotLoading()
      folderRowByExactLabel(folderLabel)
    },

    expectSidebarFolderUnderParent(
      parentFolderLabel: string,
      childFolderLabel: string
    ) {
      pageIsNotLoading()
      expandFolderRowIfCollapsed(parentFolderLabel)
      cy.get('aside').within(() => {
        folderRowByExactLabel(parentFolderLabel)
          .find('ul.daisy-list-group .sidebar-folder-label')
          .filter((_, el) => el.textContent?.trim() === childFolderLabel.trim())
          .should('have.length.at.least', 1)
      })
    },

    /**
     * One path segment after the notebook card: expand a matching folder row if present,
     * otherwise navigate by clicking the note `.title-text` row.
     */
    navigateStructuralIntermediate(segment: string) {
      pageIsNotLoading()
      const label = segment.trim()

      cy.get('aside')
        .then(($aside) => {
          const root = $aside.get(0)
          if (!root) throw new Error('aside not found')
          return [...root.querySelectorAll('.sidebar-folder-label')].some(
            (el) => el.textContent?.trim() === label
          )
        })
        .then((hasMatchingFolderRow) => {
          if (hasMatchingFolderRow) {
            expandFolderRowIfCollapsed(segment)
            return
          }
          cy.get('aside').within(() => {
            cy.get('.title-text')
              .filter((_, el) => el.textContent?.trim() === label)
              .first()
              .should('be.visible')
              .click()
          })
          pageIsNotLoading()
        })
    },

    expandStructuralIntermediateFolderOnly(segment: string) {
      expandFolderRowIfCollapsed(segment)
    },

    navigateToNote(noteTopology: string) {
      pageIsNotLoading()
      const label = noteTopology.trim()
      cy.get('aside').within(() => {
        cy.get('.title-text')
          .filter((_, el) => el.textContent?.trim() === label)
          .first()
          .should('be.visible')
          .click()
      })
      pageIsNotLoading()
    },

    /** Child note rows (`li` with `.title-text`) under an expanded sidebar folder, in DOM order. */
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

      cy.get('aside').within(() => {
        if (expected.length === 0) {
          folderRowByExactLabel(folderLabel)
            .children('ul.daisy-list-group')
            .should('not.exist')
          return
        }

        folderRowByExactLabel(folderLabel)
          .children('ul.daisy-list-group')
          .first()
          .should('be.visible')
          .children('li')
          .filter((_, li) => Cypress.$(li).find('.title-text').length > 0)
          .should('have.length', expected.length)
          .as('folderNoteRows')

        children.forEach((elem, noteIndex) => {
          cy.get('@folderNoteRows')
            .eq(noteIndex)
            .within(() => {
              for (const propName in elem) {
                const value = elem[propName]!
                if (propName === 'note-title' || propName === 'Title') {
                  cy.get('.title-text')
                    .filter((_i, el) => el.textContent?.trim() === value)
                    .should('have.length.at.least', 1)
                } else {
                  cy.findByText(value)
                }
              }
            })
        })
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
