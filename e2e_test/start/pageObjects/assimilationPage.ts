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
    cy.formField('Remember Spelling').check()
    cy.pageIsNotLoading()
    cy.findByRole('button', { name: 'Keep for repetition' }).click()
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
            const [linkType, targetNote] = commonSenseSplit(
              additionalInfo,
              '; '
            )
            if (typeof title === 'string') {
              cy.findByText(title, { selector: '[role=title] *' })
            }

            if (typeof targetNote === 'string') {
              cy.findByText(targetNote)
            }

            if (typeof linkType === 'string') {
              cy.get('.link-type').contains(linkType)
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
        cy.findByText('Keep for repetition').click()
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
  waitForNote(noteTitle: string) {
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
  expectSummaryPoints(points: string[]) {
    cy.pageIsNotLoading()
    // Wait for the summary to be generated and displayed
    cy.get('[data-test="note-details-summary"]', { timeout: 10000 }).should(
      'be.visible'
    )
    points.forEach((point) => {
      cy.get('[data-test="note-details-summary"]').should('contain', point)
    })
    return this
  },
  expectSummaryPointsAtMost(maxPoints: number) {
    cy.pageIsNotLoading()
    // Wait for the summary to be generated and displayed
    cy.get('[data-test="note-details-summary"]', { timeout: 10000 }).should(
      'be.visible'
    )
    // Count the number of list items (summary points) in the summary
    cy.get('[data-test="note-details-summary"]')
      .find('ul li')
      .should('have.length.at.most', maxPoints)
    return this
  },
  expectSummaryNotShown() {
    cy.pageIsNotLoading()
    cy.get('[data-test="note-details-summary"]').should('not.exist')
    return this
  },
  expectSummaryText(expectedText: string) {
    cy.pageIsNotLoading()
    cy.get('[data-test="note-details-summary"]', { timeout: 10000 }).should(
      'be.visible'
    )
    cy.get('[data-test="note-details-summary"]').should('contain', expectedText)
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
