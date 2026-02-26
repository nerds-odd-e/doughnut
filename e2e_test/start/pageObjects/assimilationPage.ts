import { commonSenseSplit } from 'support/string_util'
import { form } from '../forms'
import router from '../router'

const keepForRepetitionButton = (options?: { timeout?: number }) =>
  cy.get('[data-test="keep-for-repetition"]', options ?? {})

const understandingChecklist = () =>
  cy.contains('Understanding Checklist:').closest('.daisy-bg-accent')

export const assumeAssimilationPage = () => ({
  expectToAssimilateAndTotal(toAssimilateAndTotal: string) {
    const [assimilatedTodayCount, toAssimilateCountForToday, totalCount] =
      toAssimilateAndTotal.split('/')

    cy.get('.daisy-progress-bar').should(
      'contain',
      `Assimilating: ${assimilatedTodayCount}/${toAssimilateCountForToday}`
    )
    // Click progress bar to show tooltip
    cy.get('.daisy-progress-bar').first().click()

    // Check tooltip content
    cy.get('.tooltip-content').within(() => {
      cy.contains(
        `Daily Progress: ${assimilatedTodayCount} / ${toAssimilateCountForToday}`
      )
      cy.contains(`Total Progress: ${assimilatedTodayCount} / ${totalCount}`)
    })

    // Close tooltip
    cy.get('.tooltip-popup').click()
  },
  clickKeepForRepetition() {
    keepForRepetitionButton().click()
    return this
  },
  waitForAssimilationReady() {
    keepForRepetitionButton({ timeout: 10000 })
      .scrollIntoView()
      .should('be.visible')
    return this
  },
  proceedWithRememberingSpelling() {
    this.checkRememberSpellingOption()
    this.clickKeepForRepetition()
    return this
  },
  assimilateWithSpellingOption() {
    cy.get('[data-test="note-title"]')
      .first()
      .invoke('text')
      .then((noteTitle: string) => {
        this.proceedWithRememberingSpelling()
        this.verifySpellingWith(noteTitle.trim())
        this.expectPopupClosed()
        cy.pageIsNotLoading()
      })
    return this
  },
  assimilateOneNote({
    'Assimilation Type': assimilationType,
    Title: title,
    'Additional Info': additionalInfo,
    Skip: skip,
  }: Record<string, string>) {
    if (assimilationType === 'assimilation done') {
      cy.contains("You've achieved your daily assimilation goal").should(
        'be.visible'
      )
    } else {
      switch (assimilationType) {
        case 'single note': {
          if (title) cy.findByText(title, { selector: '[role=title]' })
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
            if (title) cy.findByText(title, { selector: '[role=title] *' })
            if (targetNote) cy.findByText(targetNote)
            if (relationType) cy.get('.relation-type').contains(relationType)
          }
          break
        }

        default:
          expect(assimilationType).equal('a known assimilation page type')
      }
      if (skip === 'yes') {
        cy.findByText('Skip repetition').click()
        cy.findByRole('button', { name: 'OK' }).click()
      } else {
        this.clickKeepForRepetition()
      }
    }
    return this
  },
  assimilate(assimilations: Record<string, string>[]) {
    assimilations.forEach((assimilation) => {
      this.assimilateOneNote(assimilation)
    })
  },
  assimilateNotes(noteTitles: string) {
    return this.assimilate(
      commonSenseSplit(noteTitles, ', ').map((title: string) => {
        return {
          'Assimilation Type':
            title === 'end' ? 'assimilation done' : 'single note',
          Title: title,
        }
      })
    )
  },
  expectUnderstandingPointsCount(count: number) {
    cy.pageIsNotLoading()
    understandingChecklist().scrollIntoView().should('be.visible')
    understandingChecklist().find('ul li').should('have.length', count)
    return this
  },
  expectUnderstandingChecklistNotShown() {
    cy.pageIsNotLoading()
    cy.contains('Understanding Checklist:').should('not.exist')
    return this
  },
  assimilateCurrentNote() {
    cy.pageIsNotLoading()
    this.clickKeepForRepetition()
    return this
  },
  checkUnderstandingPoint(index: number) {
    understandingChecklist().find('input[type="checkbox"]').eq(index).check()
    return this
  },
  ignoreUnderstandingPointsAndComplete(pointTexts: string[]) {
    understandingChecklist().within(() => {
      pointTexts.forEach((pointText) => {
        cy.contains('li', pointText).find('input[type="checkbox"]').check()
      })
    })
    this.clickIgnoreQuestionsButton()
    cy.findByRole('button', { name: 'OK' }).click()
    this.assimilateCurrentNote()
    return this
  },
  deleteUnderstandingPointsAt(indices: number[]) {
    indices.forEach((index) => this.checkUnderstandingPoint(index))
    cy.findByRole('button', { name: 'Delete selected points' }).click()
    cy.findByRole('button', { name: 'OK' }).click()
    return this
  },
  clickIgnoreQuestionsButton() {
    cy.findByRole('button', { name: 'Ignore questions' }).click()
    return this
  },
  promotePointToChildNote(pointText: string) {
    understandingChecklist().within(() => {
      cy.contains('li', pointText)
        .findByRole('button', { name: 'Child' })
        .click()
    })
    return this
  },
  promotePointToSiblingNote(pointText: string) {
    understandingChecklist().within(() => {
      cy.contains('li', pointText)
        .findByRole('button', { name: 'Sibling' })
        .click()
    })
    return this
  },
  checkRememberSpellingOption() {
    form.getField('Remember Spelling').check()
    cy.pageIsNotLoading()
    return this
  },
  verifySpellingWith(text: string) {
    cy.get('[data-test="spelling-verification-popup"]').should('be.visible')
    cy.get('[data-test="spelling-verification-input"]')
      .should('be.visible')
      .type(text)
    cy.get('[data-test="verify-spelling"]').click()
  },
  expectPopupClosed() {
    cy.get('[data-test="spelling-verification-popup"]').should('not.exist')
  },
  expectSpellingErrorMessage(message: string) {
    cy.get('[data-test="spelling-error-message"]').should(
      'contain.text',
      message
    )
  },
  expectRememberingSpellingUnavailable() {
    form
      .getField('Remember Spelling')
      .expectError('Remember spelling note need to have detail')
      .shouldBeDisabled()
    return this
  },
  expectRememberingSpellingAvailable() {
    form.getField('Remember Spelling').expectNoError().shouldNotBeDisabled()
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
    navigateToAssimilationPage() {
      router().toRoot()
      getAssimilateListItemInSidebar(($el) => {
        $el.click()
      })
      return assumeAssimilationPage()
    },
  }
}
