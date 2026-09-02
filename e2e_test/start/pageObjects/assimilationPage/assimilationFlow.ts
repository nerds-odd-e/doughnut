import { commonSenseSplit } from 'support/string_util'
import { waitUntilAppIsNotBusy } from '../../pageBase'
import { assimilationPropertyMemoryTrackerExpectations } from './propertyMemoryTrackerExpectations'
import { assimilationRefinementLayoutExpectations } from './refinementLayoutExpectations'
import {
  assimilateButton,
  assimilateButtonSelector,
  assimilateCommissionedButton,
  assimilateSpellingButton,
  assimilationModesSelector,
  mainNoteHeadingTitleSelector,
  expectOtherNoteLevelSecondaryActionsAbsent,
  noteLevelControlElements,
  noteLevelTrackerStatusElement,
  openRefineNoteModalIfNeeded,
  skipButton,
  returnToSequenceButton,
  waitForAssimilationNoteTitle,
  type NoteLevelTrackerKind,
} from './shared'

function expectNoteLevelTrackerStatus(kind: NoteLevelTrackerKind) {
  noteLevelTrackerStatusElement(kind).should('exist')
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
    cy.get(assimilationModesSelector, { timeout: 10000 })
      .scrollIntoView()
      .should('be.visible')
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
    assimilateCommissionedButton().click()
    waitUntilAppIsNotBusy()
    return this
  },
  rememberSpelling() {
    assimilateSpellingButton().click()
    cy.get('[data-test="spelling-verification-popup"]').should('be.visible')
    return this
  },
  expectOrdinaryAndCommissionedMemoryTrackers() {
    expectNoteLevelTrackerStatus('understanding')
    expectNoteLevelTrackerStatus('commissioned')
    return this
  },
  expectSpellingMemoryTracker() {
    expectNoteLevelTrackerStatus('spelling')
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
    // An understanding tracker already exists: the row shows a status link
    // instead of the Assimilate action, so the button is absent rather than
    // merely disabled. `cy.get(selector).filter(...)` throws instead of
    // resolving to an empty set once the raw selector matches nothing
    // anywhere in the DOM, so check element count via the document instead.
    cy.document().then((doc) => {
      expect(
        noteLevelControlElements(doc, assimilateButtonSelector)
      ).to.have.length(0)
    })
    return this
  },
  expectAssimilateEnabled() {
    assimilateButton().should('be.visible').and('not.be.disabled')
    return this
  },
})
