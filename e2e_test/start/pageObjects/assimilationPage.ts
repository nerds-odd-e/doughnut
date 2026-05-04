import { commonSenseSplit } from 'support/string_util'
import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import router from '../router'

const keepForRecallButton = (options?: { timeout?: number }) =>
  cy.get('[data-test="keep-for-recall"]', options ?? {})

const understandingChecklist = () =>
  cy
    .get('[data-test="refine-note-modal"]')
    .contains('Understanding Checklist:')
    .closest('.daisy-bg-accent')

const mainNoteTitle = () =>
  cy.get('#main-note-content [data-test="note-title"]', { timeout: 15000 })

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
  clickKeepForRecall() {
    keepForRecallButton().click()
    return this
  },
  openRefineNoteModal() {
    cy.get('[data-test="open-refine-note-modal"]').scrollIntoView().click()
    cy.get('[data-test="refine-note-modal"].daisy-modal-open').should('exist')
    pageIsNotLoading()
    return this
  },
  waitForAssimilationReady() {
    keepForRecallButton({ timeout: 10000 })
      .scrollIntoView()
      .should('be.visible')
    return this
  },
  proceedWithRememberingSpelling() {
    this.checkRememberSpellingOption()
    this.clickKeepForRecall()
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
        pageIsNotLoading()
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
          if (title) mainNoteTitle().should('contain', title)
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
            if (title) mainNoteTitle().should('contain', title)
            if (targetNote) mainNoteTitle().should('contain', targetNote)
            if (relationType) mainNoteTitle().should('contain', relationType)
          }
          break
        }

        default:
          expect(assimilationType).equal('a known assimilation page type')
      }
      if (skip === 'yes') {
        cy.findByText('Skip recall').click()
        cy.findByRole('button', { name: 'OK' }).click()
      } else {
        this.clickKeepForRecall()
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
    this.openRefineNoteModal()
    understandingChecklist().scrollIntoView().should('be.visible')
    understandingChecklist().find('ul li').should('have.length', count)
    return this
  },
  expectUnderstandingChecklistNotShown() {
    pageIsNotLoading()
    cy.contains('Understanding Checklist:').should('not.exist')
    return this
  },
  assimilateCurrentNote() {
    pageIsNotLoading()
    this.clickKeepForRecall()
    return this
  },
  checkUnderstandingPoint(index: number) {
    understandingChecklist().find('input[type="checkbox"]').eq(index).check()
    return this
  },
  ignoreUnderstandingPointsAndComplete(pointTexts: string[]) {
    this.openRefineNoteModal()
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
    this.openRefineNoteModal()
    indices.forEach((index) => this.checkUnderstandingPoint(index))
    cy.findByRole('button', { name: 'Delete selected points' }).click()
    cy.findByRole('button', { name: 'OK' }).click()
    return this
  },
  clickIgnoreQuestionsButton() {
    cy.findByRole('button', { name: 'Ignore questions' }).click()
    return this
  },
  /** Requires the refine-note modal to already be open (e.g. after openRefineNoteModal or expectUnderstandingPointsCount). */
  promotePointToSiblingNote(pointText: string) {
    understandingChecklist().within(() => {
      cy.contains('li', pointText)
        .findByRole('button', { name: 'Sibling' })
        .click()
    })
    cy.get('[data-test="close-refine-note-modal"]').click()
    return this
  },
  checkRememberSpellingOption() {
    form.getField('Remember Spelling').check()
    pageIsNotLoading()
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
  expectKeepForRecallDisabled() {
    keepForRecallButton().should('be.disabled')
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
