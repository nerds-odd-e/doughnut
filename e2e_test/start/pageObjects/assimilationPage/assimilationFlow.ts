import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../../pageBase'
import { assimilationPropertyMemoryTrackerExpectations } from './propertyMemoryTrackerExpectations'
import { assimilationRefinementLayoutExpectations } from './refinementLayoutExpectations'
import {
  assimilateAsCommissionedButton,
  assimilateOptionsCaret,
  assimilateButton,
  mainNoteHeadingTitleSelector,
  expectOtherNoteLevelSecondaryActionsAbsent,
  openRefineNoteModalIfNeeded,
  rememberSpellingButton,
  reviveButton,
  skipButton,
  returnToSequenceButton,
  removeFromRecallButton,
  waitForAssimilationNoteTitle,
  noteLevelTrackerRowLabel,
} from './shared'

const openAssimilateOption = (
  optionButton: typeof assimilateAsCommissionedButton
) => {
  assimilateOptionsCaret().click()
  optionButton().click()
}

const chooseAssimilateOption = (
  optionButton: typeof assimilateAsCommissionedButton
) => {
  openAssimilateOption(optionButton)
  waitUntilAppIsNotBusy()
}

export const assumeAssimilationPage = () => ({
  ...assimilationPropertyMemoryTrackerExpectations(),
  ...assimilationRefinementLayoutExpectations(),
  clickAssimilate() {
    assimilateButton().click()
    return this
  },
  openRefineNoteModal() {
    openRefineNoteModalIfNeeded()
    return this
  },
  waitForAssimilationReady() {
    assimilateButton({ timeout: 10000 }).scrollIntoView().should('be.visible')
    return this
  },
  expectAssimilatingNote(title: string) {
    waitForAssimilationNoteTitle(title)
    this.waitForAssimilationReady()
    return this
  },
  assimilateOnPanel() {
    this.clickAssimilate()
    waitUntilAppIsNotBusy()
    return this
  },
  assimilateAsCommissioned() {
    chooseAssimilateOption(assimilateAsCommissionedButton)
    return this
  },
  rememberSpelling() {
    openAssimilateOption(rememberSpellingButton)
    cy.get('[data-test="spelling-verification-popup"]').should('be.visible')
    return this
  },
  expectOrdinaryAndCommissionedMemoryTrackers() {
    this.expectMemoryTrackerInfo([
      { type: noteLevelTrackerRowLabel('understanding') },
      { type: noteLevelTrackerRowLabel('commissioned') },
    ])
    return this
  },
  expectSpellingMemoryTracker() {
    this.expectMemoryTrackerInfo([
      { type: noteLevelTrackerRowLabel('spelling') },
    ])
    return this
  },
  skipOnPanel() {
    skipButton().click()
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
    return this
  },
  returnToSequenceOnPanel() {
    returnToSequenceButton().click()
    waitUntilAppIsNotBusy()
    return this
  },
  expectSkipOnPanel() {
    skipButton().should('exist')
    cy.document().then((doc) => {
      expectOtherNoteLevelSecondaryActionsAbsent(doc, 'skip')
    })
    return this
  },
  expectReturnToSequenceOnPanel() {
    returnToSequenceButton().should('exist')
    cy.document().then((doc) => {
      expectOtherNoteLevelSecondaryActionsAbsent(doc, 'returnToSequence')
    })
    return this
  },
  removeFromRecallOnPanel() {
    removeFromRecallButton().click()
    cy.findByRole('button', { name: 'OK' }).click()
    waitUntilAppIsNotBusy()
    return this
  },
  expectRemoveFromRecallOnPanel() {
    removeFromRecallButton().should('exist')
    cy.document().then((doc) => {
      expectOtherNoteLevelSecondaryActionsAbsent(doc, 'removeFromRecall')
    })
    return this
  },
  expectReviveOnPanel() {
    reviveButton().should('exist')
    cy.document().then((doc) => {
      expectOtherNoteLevelSecondaryActionsAbsent(doc, 'revive')
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

      case 'relationship': {
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
      this.skipOnPanel()
    } else {
      this.clickAssimilate()
      waitUntilAppIsNotBusy()
    }
    return this
  },
  assimilate(assimilations: Record<string, string>[]) {
    assimilations.forEach((assimilation) => {
      this.assimilateOneNote(assimilation)
    })
  },
  assimilateCurrentNote() {
    waitUntilAppIsNotBusy()
    this.clickAssimilate()
    return this
  },
  verifySpellingWith(text: string) {
    cy.get('[data-test="spelling-verification-popup"]').should('be.visible')
    cy.get('[data-test="spelling-verification-input"]')
      .should('be.visible')
      .clear()
      .type(text)
    cy.get('[data-test="verify-spelling"]').click()
    waitUntilAppIsNotBusy()
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
  expectAssimilateDisabled() {
    assimilateButton().should('be.disabled')
    return this
  },
  expectAssimilateEnabled() {
    assimilateButton().should('not.be.disabled')
    return this
  },
})
