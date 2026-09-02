import { waitUntilAppIsNotBusy } from '../../pageBase'
import { clickToolbarOverflowAction } from '../noteToolbarOverflow'

// Note-level trigger row markup: `AssimilationModes.vue` (`data-testid`
// "assimilation-modes"). Each mode gets its own direct trigger — no more
// dropdown/caret. `assimilateButtonSelector` targets the UNDERSTANDING row,
// the "plain" assimilate action historically referred to by that name.
export const assimilationModesSelector = '[data-testid="assimilation-modes"]'
export const assimilateButtonSelector = '[data-test="assimilate-UNDERSTANDING"]'
export const assimilateCommissionedSelector =
  '[data-test="assimilate-COMMISSIONED"]'
export const assimilateSpellingSelector = '[data-test="assimilate-SPELLING"]'
export const skipButtonSelector = '[data-test="skip"]'
export const returnToSequenceButtonSelector = '[data-test="return-to-sequence"]'

export const isNoteLevelAssimilationControl = (el: Element) =>
  el.closest('[data-testid="rich-note-property-row"]') === null

const noteLevelControl =
  (selector: string) => (options?: { timeout?: number }) =>
    cy
      .get(selector, options ?? {})
      .filter((_, el) => isNoteLevelAssimilationControl(el))

export const assimilateButton = noteLevelControl(assimilateButtonSelector)
export const assimilateCommissionedButton = noteLevelControl(
  assimilateCommissionedSelector
)
export const assimilateSpellingButton = noteLevelControl(
  assimilateSpellingSelector
)
export const skipButton = noteLevelControl(skipButtonSelector)
export const returnToSequenceButton = noteLevelControl(
  returnToSequenceButtonSelector
)

export function noteLevelControlElements(
  doc: Document | ParentNode,
  selector: string
): Element[] {
  return [...doc.querySelectorAll(selector)].filter(
    isNoteLevelAssimilationControl
  )
}

export const secondaryActionSelectors = {
  skip: skipButtonSelector,
  returnToSequence: returnToSequenceButtonSelector,
} as const

export type AssimilationSecondaryAction = keyof typeof secondaryActionSelectors

export function expectOtherNoteLevelSecondaryActionsAbsent(
  doc: Document | ParentNode,
  present: AssimilationSecondaryAction
) {
  for (const [name, selector] of Object.entries(secondaryActionSelectors)) {
    if (name === present) continue
    expect(noteLevelControlElements(doc, selector)).to.have.length(0)
  }
}

const refineNoteButtonTitle = 'Refine note'

export function openRefineNoteModalIfNeeded() {
  cy.get('body').then(($body) => {
    if (
      $body.find('[data-test="refine-note-modal"].daisy-modal-open').length > 0
    ) {
      waitUntilAppIsNotBusy()
      return
    }
    clickToolbarOverflowAction(refineNoteButtonTitle)
    cy.get('[data-test="refine-note-modal"].daisy-modal-open').should('exist')
    waitUntilAppIsNotBusy()
  })
}

export const refinementLayoutPanel = () =>
  cy
    .get('[data-test="refine-note-modal"]')
    .contains('Refinement layout:')
    .closest('.bg-accent')

export function layoutCheckboxForItem(layoutItemText: string) {
  return refinementLayoutPanel()
    .contains('[data-layout-level] > label', layoutItemText)
    .find('input[type="checkbox"]')
    .first()
}

export const removeRefinementLayoutButton = () =>
  refinementLayoutPanel().find('[data-test-id="remove-refinement-layout"]')

export const waitForExtractNotePreview = () => {
  cy.contains('p.loading-message', 'AI is generating preview...', {
    timeout: 15000,
  }).should('not.exist')
}

export const waitForExtractNote = () => {
  cy.contains('p.loading-message', 'AI is creating note...', {
    timeout: 15000,
  }).should('not.exist')
}

export const mainNoteHeadingTitleSelector =
  '#main-note-content h2.path-name-heading [role=title], #main-note-content [data-test="note-title"]'

export const assimilationToastMessages = {
  dailyGoalMet: "You've achieved your daily assimilation goal",
  noMoreNotes: 'No more notes to assimilate',
} as const

export function assimilationDueFromTriple(triple: string) {
  const [assimilated, planned] = triple.split('/').map(Number)
  return (planned ?? 0) - (assimilated ?? 0)
}

export function expectSuccessToast(message: string) {
  cy.contains('.Vue-Toastification__toast--success', message, {
    timeout: 10000,
  }).should('be.visible')
}

// Note-level tracker rows now come from `AssimilationModes.vue`
// (`[data-test="assimilation-mode-row-<MODE>"]`, `[data-test="assimilation-status-<MODE>"]`)
// instead of the removed Memory Trackers table.
export type NoteLevelTrackerKind = 'understanding' | 'spelling' | 'commissioned'

const noteLevelTrackerKindToMode: Record<NoteLevelTrackerKind, string> = {
  understanding: 'UNDERSTANDING',
  spelling: 'SPELLING',
  commissioned: 'COMMISSIONED',
}

export function assimilationStatusSelector(kind: NoteLevelTrackerKind): string {
  return `[data-test="assimilation-status-${noteLevelTrackerKindToMode[kind]}"]`
}

export function noteLevelTrackerStatusElement(kind: NoteLevelTrackerKind) {
  return cy
    .get(assimilationStatusSelector(kind))
    .filter((_, el) => isNoteLevelAssimilationControl(el))
}

export function waitForAssimilationNoteTitle(expectedTitle?: string) {
  waitUntilAppIsNotBusy()
  cy.get('#main-note-content', { timeout: 15000 }).should('be.visible')
  const title = cy.get(mainNoteHeadingTitleSelector, { timeout: 15000 })
  if (expectedTitle !== undefined && expectedTitle.trim() !== '') {
    title.should('contain', expectedTitle.trim())
  } else {
    title.should('exist')
  }
}
