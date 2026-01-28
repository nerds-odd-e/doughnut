import { commonSenseSplit } from 'support/string_util'
import { form } from '../forms'

export const assumeAssimilationPage = () => ({
  expectToAssimilateAndTotal(toAssimilateAndTotal: string) {
    const [assimlatedTodayCount, toAssimilateCountForToday, totalCount] =
      toAssimilateAndTotal.split('/')

    cy.get('.daisy-progress-bar').should(
      'contain',
      `Assimilating: ${assimlatedTodayCount}/${toAssimilateCountForToday}`
    )
    // Click progress bar to show tooltip
    cy.get('.daisy-progress-bar').first().click()

    // Check tooltip content
    cy.get('.tooltip-content').within(() => {
      cy.contains(
        `Daily Progress: ${assimlatedTodayCount} / ${toAssimilateCountForToday}`
      )
      cy.contains(`Total Progress: ${assimlatedTodayCount} / ${totalCount}`)
    })

    // Close tooltip
    cy.get('.tooltip-popup').click()
  },
  assimilateWithSpellingOption() {
    // Get the note title from the page before opening the popup
    cy.get('[data-test="note-title"]')
      .first()
      .invoke('text')
      .then((noteTitle: string) => {
        cy.formField('Remember Spelling').check()
        cy.pageIsNotLoading()
        cy.get('[data-test="keep-for-repetition"]').click()
        // Handle spelling verification popup
        cy.get('[data-test="spelling-verification-popup"]').should('be.visible')
        cy.get('[data-test="spelling-verification-input"]').type(
          noteTitle.trim()
        )
        cy.get('[data-test="verify-spelling"]').click()
        // Wait for popup to close after successful verification
        cy.get('[data-test="spelling-verification-popup"]').should('not.exist')
        cy.pageIsNotLoading()
      })
    return this
  },
  initialReviewOneNote({
    'Review Type': reviewType,
    Title: title,
    'Additional Info': additionalInfo,
    Skip: skip,
  }: Record<string, string>) {
    if (reviewType === 'initial done') {
      cy.contains("You've achieved your daily assimilation goal").should(
        'be.visible'
      )
    } else {
      switch (reviewType) {
        case 'single note': {
          cy.findByText(title, { selector: '[role=title]' })
          if (additionalInfo) {
            cy.get('.note-details').should('contain', additionalInfo)
          }
          break
        }

        case 'image note': {
          if (additionalInfo) {
            const [expectedDetails, expectedImage] = commonSenseSplit(
              additionalInfo,
              '; '
            )
            cy.get('.note-details').should('contain', expectedDetails)
            cy.get('#note-image')
              .find('img')
              .should('have.attr', 'src')
              .should('include', expectedImage)
          }
          break
        }

        case 'link': {
          if (additionalInfo) {
            const [relationType, targetNote] = commonSenseSplit(
              additionalInfo,
              '; '
            )
            if (typeof title === 'string') {
              cy.findByText(title, { selector: '[role=title] *' })
            }

            if (typeof targetNote === 'string') {
              cy.findByText(targetNote)
            }

            if (typeof relationType === 'string') {
              cy.get('.relation-type').contains(relationType)
            }
          }
          break
        }

        default:
          expect(reviewType).equal('a known review page type')
      }
      if (skip === 'yes') {
        cy.findByText('Skip repetition').click()
        cy.findByRole('button', { name: 'OK' }).click()
      } else {
        cy.get('[data-test="keep-for-repetition"]').click()
      }
    }
    return this
  },
  assimilate(assimilations: Record<string, string>[]) {
    assimilations.forEach((assimilation) => {
      this.initialReviewOneNote(assimilation)
    })
  },
  assimilateNotes(noteTitles: string) {
    return this.assimilate(
      commonSenseSplit(noteTitles, ', ').map((title: string) => {
        return {
          'Review Type': title === 'end' ? 'initial done' : 'single note',
          Title: title,
        }
      })
    )
  },
  expectNoteTitle(noteTitle: string) {
    cy.findByText(noteTitle, { selector: 'main *' }).should('be.visible')
    cy.pageIsNotLoading()
    return this
  },
  selectNoteType(noteType: string) {
    form.fill({ 'Note Type': noteType })
    cy.pageIsNotLoading()
    return this
  },
  expectNoteTypePrompt() {
    cy.pageIsNotLoading()
    cy.get('[data-test="note-type-selection-dialog"]', {
      timeout: 5000,
    }).should('be.visible')
    return this
  },
  expectNoteTypeOptions(options: string[]) {
    options.forEach((option) => {
      cy.get('[data-test="note-type-selection-dialog"]').within(() => {
        cy.findByRole('button', { name: option }).should('be.visible')
      })
    })
    return this
  },
  expectUnderstandingPointsAtMost(maxPoints: number) {
    cy.pageIsNotLoading()
    // Find the understanding checklist by its heading text
    cy.contains('Understanding Checklist:')
      .closest('.daisy-bg-accent')
      .should('be.visible')
    // Count the number of list items (understanding points) in the checklist
    cy.contains('Understanding Checklist:')
      .closest('.daisy-bg-accent')
      .find('ul li')
      .should('have.length.at.most', maxPoints)
    return this
  },
  expectUnderstandingChecklistNotShown() {
    cy.pageIsNotLoading()
    cy.contains('Understanding Checklist:').should('not.exist')
    return this
  },
  assimilateCurrentNote() {
    cy.pageIsNotLoading()
    cy.get('[data-test="keep-for-repetition"]').click()
    return this
  },
  checkUnderstandingPoint(index: number) {
    cy.contains('Understanding Checklist:')
      .closest('.daisy-bg-accent')
      .find('input[type="checkbox"]')
      .eq(index)
      .check()
    return this
  },
  clickDeleteUnderstandingPointsButton() {
    cy.findByRole('button', { name: 'Delete selected points' }).click()
    return this
  },
  promotePointToChildNote(pointText: string) {
    cy.contains('li', pointText).find('button').click()
    return this
  },
  expectToRemainOnAssimilationPageFor(noteTitle: string) {
    cy.url().should('include', '/assimilate')
    cy.findByText(noteTitle).should('exist')
    return this
  },
  expectPointRemovedFromChecklist(pointText: string) {
    cy.contains('li', pointText).should('not.exist')
    return this
  },
  expectChecklistHasRemainingPoints() {
    cy.get('ul').find('li').should('have.length.at.least', 1)
    return this
  },
})

export const assimilation = () => {
  const getAssimilateListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Assimilate"]')))

  return {
    expectCount(numberOfNotes: number) {
      getAssimilateListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.due-count' })
      })
      return this
    },
    goToAssimilationPage() {
      cy.routerToRoot()
      getAssimilateListItemInSidebar(($el) => {
        $el.click()
      })
      return assumeAssimilationPage()
    },
  }
}
