import { pageIsNotLoading } from '../pageBase'
import noteCreationForm from './noteForms/noteCreationForm'

const sidebarActionTimeoutMs = 20000

const trimmedText = (el: Element) => el.textContent?.trim() ?? ''

/** Cypress/jQuery filter: element text equals `label` after trim. */
const matchesExactLabel = (label: string) => (_: number, el: HTMLElement) =>
  trimmedText(el) === label.trim()

type AsideSegmentKind = 'folder' | 'note'

function asideSegmentKind(
  aside: HTMLElement,
  label: string
): AsideSegmentKind | null {
  const t = label.trim()
  if (
    [...aside.querySelectorAll('.sidebar-folder-label')].some(
      (el) => trimmedText(el) === t
    )
  ) {
    return 'folder'
  }
  if (
    [...aside.querySelectorAll('.title-text')].some(
      (el) => trimmedText(el) === t
    )
  ) {
    return 'note'
  }
  return null
}

function withinAside(fn: () => void) {
  cy.get('aside').within(fn)
}

function clickSidebarNoteTitleExact(label: string) {
  const t = label.trim()
  withinAside(() => {
    cy.get('.title-text', { timeout: sidebarActionTimeoutMs })
      .filter(matchesExactLabel(t))
      .should('have.length.at.least', 1)
      .first()
      .should('be.visible')
      .click()
  })
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

function openSidebarIfCollapsed() {
  cy.findByRole('button', { name: 'toggle sidebar' }).then(($button) => {
    if (!$button.hasClass('sidebar-expanded')) {
      cy.wrap($button).click()
    }
  })
  cy.get('aside').should('be.visible')
}

/** Deepest visible `.sidebar-folder-label` row matching `folderLabel`. */
function folderRowByExactLabel(folderLabel: string) {
  return cy
    .get('.sidebar-folder-label')
    .filter(matchesExactLabel(folderLabel))
    .filter(':visible')
    .last()
    .should('be.visible')
    .closest('li')
}

function expandFolderRowIfCollapsed(segment: string) {
  pageIsNotLoading()
  const label = segment.trim()
  withinAside(() => {
    folderRowByExactLabel(label).then(($li) => {
      if ($li.attr('data-sidebar-folder-expanded') !== 'true') {
        cy.wrap($li).within(() => {
          cy.findByTitle('expand children').click()
        })
      }
    })
  })
  pageIsNotLoading()
  withinAside(() => {
    folderRowByExactLabel(label).within(() => {
      cy.get('ul.daisy-list-group li, .title-text', {
        timeout: sidebarActionTimeoutMs,
      }).should('have.length.at.least', 1)
    })
  })
}

function newNoteSidebarButton() {
  pageIsNotLoading()
  return sidebarAddNoteButton('New note')
}

export const noteSidebar = () => {
  openSidebarIfCollapsed()

  return {
    expand: (noteTopology: string) => expandFolderRowIfCollapsed(noteTopology),

    siblingOrder: (higher: string, lower: string) => {
      withinAside(() => {
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

    addingNoteButton: newNoteSidebarButton,

    addingChildNoteButton: newNoteSidebarButton,

    addingChildNote() {
      sidebarAddNoteButton('New note').click()
      return noteCreationForm
    },

    activateFolderByLabel(folderLabel: string) {
      pageIsNotLoading()
      withinAside(() => {
        folderRowByExactLabel(folderLabel)
          .find('.sidebar-folder-label')
          .should('be.visible')
          .click()
      })
      pageIsNotLoading()
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
      folderRowByExactLabel(folderLabel)
    },

    expectSidebarFolderUnderParent(
      parentFolderLabel: string,
      childFolderLabel: string
    ) {
      pageIsNotLoading()
      expandFolderRowIfCollapsed(parentFolderLabel)
      withinAside(() => {
        folderRowByExactLabel(parentFolderLabel)
          .find('ul.daisy-list-group .sidebar-folder-label')
          .filter(matchesExactLabel(childFolderLabel))
          .should('have.length.at.least', 1)
      })
    },

    /**
     * After the notebook card: expand a folder row for this segment, or open the note row.
     * Waits for sidebar hydration so a folder label is not mistaken for a missing note title.
     */
    navigateStructuralIntermediate(segment: string) {
      pageIsNotLoading()
      const label = segment.trim()

      cy.get('aside', { timeout: sidebarActionTimeoutMs }).should(($aside) => {
        const root = $aside.get(0)
        expect(root, 'aside').to.exist
        expect(
          asideSegmentKind(root!, label),
          `sidebar should load "${label}" as folder or note row`
        ).to.not.be.null
      })

      cy.get('aside').then(($aside) => {
        const root = $aside.get(0)!
        if (asideSegmentKind(root, label) === 'folder') {
          expandFolderRowIfCollapsed(segment)
        } else {
          clickSidebarNoteTitleExact(label)
          pageIsNotLoading()
        }
      })
    },

    expandStructuralIntermediateFolderOnly(segment: string) {
      expandFolderRowIfCollapsed(segment)
    },

    navigateToNote(noteTopology: string) {
      pageIsNotLoading()
      clickSidebarNoteTitleExact(noteTopology)
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

      withinAside(() => {
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
                    .filter(matchesExactLabel(value))
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
