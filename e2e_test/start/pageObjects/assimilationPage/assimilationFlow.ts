import { commonSenseSplit } from 'support/string_util'
import { pageIsNotLoading } from '../../pageBase'
import { form } from '../../forms'
import { assimilationPropertyMemoryTrackerExpectations } from './propertyMemoryTrackerExpectations'
import {
  keepForRecallButton,
  mainNoteHeadingTitleSelector,
  refinementSuggestionsPanel,
  waitForAssimilationNoteTitle,
  waitForExtractNote,
} from './shared'

export const assumeAssimilationPage = () => ({
  ...assimilationPropertyMemoryTrackerExpectations(),
  expectAssimilationProgressSummary(triple: string) {
    cy.get('[data-test="assimilation-progress-summary"]')
      .should('be.visible')
      .and('contain', triple.trim())
    return this
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
  expectAssimilatingNote(title: string) {
    waitForAssimilationNoteTitle(title)
    this.waitForAssimilationReady()
    return this
  },
  keepForRecallOnPanel() {
    this.clickKeepForRecall()
    pageIsNotLoading()
    return this
  },
  skipRecallOnPanel() {
    cy.findByText('Skip recall').click()
    cy.findByRole('button', { name: 'OK' }).click()
    pageIsNotLoading()
    return this
  },
  proceedWithRememberingSpelling() {
    this.waitForAssimilationReady()
    this.checkRememberSpellingOption()
    this.clickKeepForRecall()
    return this
  },
  assimilateWithSpellingOption() {
    waitForAssimilationNoteTitle()
    cy.get(mainNoteHeadingTitleSelector)
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
    switch (assimilationType) {
      case 'single note': {
        waitForAssimilationNoteTitle(title)
        this.waitForAssimilationReady()
        if (additionalInfo) {
          cy.get('.note-content').should('contain', additionalInfo)
        }
        break
      }

      case 'image note': {
        waitForAssimilationNoteTitle()
        this.waitForAssimilationReady()
        if (additionalInfo) {
          const [expectedBodyText, expectedImage] = commonSenseSplit(
            additionalInfo,
            '; '
          )
          cy.get('.note-content').should('contain', expectedBodyText)
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
          if (title) waitForAssimilationNoteTitle(title)
          if (targetNote) waitForAssimilationNoteTitle(targetNote)
          if (relationType) {
            cy.get(mainNoteHeadingTitleSelector).should('contain', relationType)
          }
          this.waitForAssimilationReady()
        }
        break
      }

      default:
        expect(assimilationType).equal('a known assimilation page type')
    }
    if (skip === 'yes') {
      this.skipRecallOnPanel()
    } else {
      this.clickKeepForRecall()
      pageIsNotLoading()
    }
    return this
  },
  assimilate(assimilations: Record<string, string>[]) {
    assimilations.forEach((assimilation) => {
      this.assimilateOneNote(assimilation)
    })
  },
  expectRefinementSuggestionsCount(count: number) {
    this.openRefineNoteModal()
    refinementSuggestionsPanel().scrollIntoView().should('be.visible')
    refinementSuggestionsPanel().find('ul li').should('have.length', count)
    return this
  },
  assimilateCurrentNote() {
    pageIsNotLoading()
    this.clickKeepForRecall()
    return this
  },
  checkRefinementSuggestion(index: number) {
    refinementSuggestionsPanel()
      .find('input[type="checkbox"]')
      .eq(index)
      .check()
    return this
  },
  removeRefinementSuggestionsAt(indices: number[]) {
    this.openRefineNoteModal()
    indices.forEach((index) => this.checkRefinementSuggestion(index))
    cy.findByRole('button', { name: 'Remove selected' }).click()
    cy.findByRole('button', { name: 'OK' }).click()
    return this
  },
  /** Requires the refine-note modal to already be open (e.g. after openRefineNoteModal or expectRefinementSuggestionsCount). */
  extractSuggestionToNewNote(suggestionText: string) {
    refinementSuggestionsPanel().within(() => {
      cy.contains('li', suggestionText)
        .findByRole('button', { name: 'Extract note' })
        .click()
    })
    waitForExtractNote()
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
      .clear()
      .type(text)
    cy.get('[data-test="verify-spelling"]').click()
  },
  expectPopupClosed() {
    cy.get('[data-test="spelling-verification-popup"]', {
      timeout: 15000,
    }).should('not.exist')
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
      .expectError('Remember spelling note need to have content')
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
  expectKeepForRecallEnabled() {
    keepForRecallButton().should('not.be.disabled')
    return this
  },
  expandAssimilationPropertiesSection() {
    cy.get('[data-test="assimilation-properties-section"]').within(() => {
      cy.get('[data-test="assimilation-properties-toggle"]').click()
    })
    return this
  },
  expectPendingAssimilationProperty(propertyKey: string) {
    cy.get('[data-test="assimilation-properties-section"]').within(() => {
      cy.get('[data-test="assimilation-properties-toggle"]').should(
        'be.checked'
      )
      cy.get(
        `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
      )
        .should('have.attr', 'data-test-pending', 'true')
        .and('be.visible')
    })
    return this
  },
  expectPendingAssimilationPropertyAbsent(propertyKey: string) {
    cy.get(
      `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
    ).should('not.have.attr', 'data-test-pending', 'true')
    return this
  },
  assimilateProperty(propertyKey: string) {
    cy.get(
      `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
    ).within(() => {
      cy.get('[data-test="keep-for-recall"]').click()
    })
    pageIsNotLoading()
    return this
  },
  skipRecallProperty(propertyKey: string) {
    cy.get(
      `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
    ).within(() => {
      cy.findByText('Skip recall').click()
    })
    cy.findByRole('button', { name: 'OK' }).click()
    pageIsNotLoading()
    return this
  },
  expectPropertyKeepForRecallDisabled(propertyKey: string) {
    cy.get(
      `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
    ).within(() => {
      cy.get('[data-test="keep-for-recall"]').should('be.disabled')
    })
    return this
  },
  expectPropertyKeepForRecallEnabled(propertyKey: string) {
    cy.get(
      `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
    ).within(() => {
      cy.get('[data-test="keep-for-recall"]').should('not.be.disabled')
    })
    return this
  },
})
