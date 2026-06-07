import { UserController } from '@generated/doughnut-backend-api/sdk.gen'
import type { MenuDataDto } from '@generated/doughnut-backend-api'
import { commonSenseSplit } from 'support/string_util'
import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import { assumeMemoryTrackerPage } from './memoryTrackerPage'

export const keepForRecallButton = (options?: { timeout?: number }) =>
  cy.get('[data-test="keep-for-recall"]', options ?? {})

const refinementSuggestionsPanel = () =>
  cy
    .get('[data-test="refine-note-modal"]')
    .contains('Refinement suggestions:')
    .closest('.bg-accent')

const waitForExtractNote = () => {
  cy.contains('p.loading-message', 'AI is creating note...', {
    timeout: 15000,
  }).should('not.exist')
}

const mainNoteHeadingTitleSelector =
  '#main-note-content h2.path-name-heading [role=title], #main-note-content [data-test="note-title"]'

const assimilationToastMessages = {
  dailyGoalMet: "You've achieved your daily assimilation goal",
  noMoreNotes: 'No more notes to assimilate',
} as const

function assimilationDueFromTriple(triple: string) {
  const [assimilated, planned] = triple.split('/').map(Number)
  return (planned ?? 0) - (assimilated ?? 0)
}

function expectSuccessToast(message: string) {
  cy.contains('.Vue-Toastification__toast--success', message, {
    timeout: 10000,
  }).should('be.visible')
}

function propertyMemoryTrackerRowLabel(propertyKey: string) {
  return `property: ${propertyKey}`
}

function waitForAssimilationNoteTitle(expectedTitle?: string) {
  pageIsNotLoading()
  cy.get('#main-note-content', { timeout: 15000 }).should('be.visible')
  const title = cy.get(mainNoteHeadingTitleSelector, { timeout: 15000 })
  if (expectedTitle !== undefined && expectedTitle.trim() !== '') {
    title.should('contain', expectedTitle.trim())
  } else {
    title.should('exist')
  }
}

export const assumeAssimilationPage = () => ({
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
  expandAssimilationPropertiesSection() {
    cy.get('[data-test="assimilation-properties-section"]').within(() => {
      cy.get('[data-test="assimilation-properties-toggle"]').click()
    })
    return this
  },
  assimilateProperty(propertyKey: string) {
    cy.get(
      `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
    ).within(() => {
      cy.findByRole('button', { name: 'Assimilate' }).click()
    })
    pageIsNotLoading()
    return this
  },
  expectPropertyMemoryTracker(propertyKey: string, recallCount = 0) {
    this.expectMemoryTrackerInfo([
      {
        type: propertyMemoryTrackerRowLabel(propertyKey),
        'Recall Count': String(recallCount),
      },
    ])
    return this
  },
  expectPropertyMemoryTrackerAbsent(propertyKey: string) {
    cy.contains('tr', propertyMemoryTrackerRowLabel(propertyKey)).should(
      'not.exist'
    )
    return this
  },
  openPropertyMemoryTracker(propertyKey: string) {
    cy.contains('tr', propertyMemoryTrackerRowLabel(propertyKey)).click()
    cy.url().should('include', '/memory-trackers/')
    pageIsNotLoading()
    return assumeMemoryTrackerPage()
  },
  expectMemoryTrackerInfo(expected: { [key: string]: string }[]) {
    for (const k in expected) {
      cy.contains('tr', expected[k]?.type ?? '').within(() => {
        for (const attr in expected[k]) {
          if (expected[k][attr] !== undefined) {
            cy.contains('td', expected[k][attr])
          }
        }
      })
    }
    return this
  },
  removeMemoryTrackerFromRecall(type: 'normal' | 'spelling') {
    cy.contains('tr', type).click()
    cy.url().should('include', '/memory-trackers/')
    pageIsNotLoading()
    return assumeMemoryTrackerPage().removeFromRecall()
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
    expectAssimilationDueFromTriple(toAssimilateAndTotal: string) {
      const expectedDue = assimilationDueFromTriple(toAssimilateAndTotal)
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      cy.wrap(UserController.getMenuData({ query: { timezone } }), {
        log: false,
      }).then((menuData: MenuDataDto) => {
        expect(menuData.assimilationCount?.dueCount ?? 0).to.eq(expectedDue)
      })
      return this
    },
    startAssimilationFromMenu() {
      getAssimilateListItemInSidebar(($el) => {
        $el.click()
      })
      pageIsNotLoading()
      return this
    },
    expectDailyAssimilationGoalToast() {
      expectSuccessToast(assimilationToastMessages.dailyGoalMet)
      return this
    },
    expectNoMoreNotesToAssimilateToast() {
      expectSuccessToast(assimilationToastMessages.noMoreNotes)
      return this
    },
    expectAssimilationMenuProgress() {
      getAssimilateListItemInSidebar(($el) => {
        $el
          .find('[data-testid="assimilation-menu-progress"]')
          .should('be.visible')
      })
      return this
    },
  }
}
