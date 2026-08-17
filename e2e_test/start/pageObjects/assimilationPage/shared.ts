import { waitUntilAppIsNotBusy } from '../../pageBase'

export const assimilateButtonSelector = '[data-test="assimilate"]'
export const reviveButtonSelector = '[data-test="revive"]'
export const skipButtonSelector = '[data-test="skip"]'
export const returnToSequenceButtonSelector = '[data-test="return-to-sequence"]'
export const removeFromRecallButtonSelector = '[data-test="remove-from-recall"]'
export const assimilateOptionsCaretSelector =
  '[data-test="assimilate-options-caret"]'
export const assimilateAsCommissionedSelector =
  '[data-test="assimilate-as-commissioned"]'
export const rememberSpellingSelector = '[data-test="remember-spelling"]'

export const assimilationPropertyRow = (propertyKey: string) =>
  cy.get(
    `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
  )

export const isNoteLevelAssimilationControl = (el: Element) =>
  el.closest('[data-test="assimilation-property-row"]') === null

const noteLevelControl =
  (selector: string) => (options?: { timeout?: number }) =>
    cy
      .get(selector, options ?? {})
      .filter((_, el) => isNoteLevelAssimilationControl(el))

export const assimilateButton = noteLevelControl(assimilateButtonSelector)
export const assimilateOptionsCaret = noteLevelControl(
  assimilateOptionsCaretSelector
)
export const assimilateAsCommissionedButton = noteLevelControl(
  assimilateAsCommissionedSelector
)
export const rememberSpellingButton = noteLevelControl(rememberSpellingSelector)
export const reviveButton = noteLevelControl(reviveButtonSelector)
export const skipButton = noteLevelControl(skipButtonSelector)
export const returnToSequenceButton = noteLevelControl(
  returnToSequenceButtonSelector
)
export const removeFromRecallButton = noteLevelControl(
  removeFromRecallButtonSelector
)

function noteLevelControlElements(
  doc: Document | ParentNode,
  selector: string
): Element[] {
  return [...doc.querySelectorAll(selector)].filter(
    isNoteLevelAssimilationControl
  )
}

export const secondaryActionSelectors = {
  revive: reviveButtonSelector,
  skip: skipButtonSelector,
  returnToSequence: returnToSequenceButtonSelector,
  removeFromRecall: removeFromRecallButtonSelector,
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

export function openRefineNoteModalIfNeeded() {
  cy.get('[data-test="refine-note-modal"]').then(($modal) => {
    if ($modal.hasClass('daisy-modal-open')) {
      waitUntilAppIsNotBusy()
      return
    }
    cy.get('[data-test="open-refine-note-modal"]').scrollIntoView().click()
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

export function propertyMemoryTrackerRowLabel(propertyKey: string) {
  return `property: ${propertyKey}`
}

// Assimilation table labels from NoteInfoMemoryTracker.vue trackerTypeLabel:
// understanding → 'normal', commissioned → 'Commissioned'.
export type NoteLevelTrackerKind = 'understanding' | 'spelling' | 'commissioned'

const noteLevelTrackerRowLabels: Record<NoteLevelTrackerKind, string> = {
  understanding: 'normal',
  spelling: 'spelling',
  commissioned: 'Commissioned',
}

export function noteLevelTrackerRowLabel(kind: NoteLevelTrackerKind): string {
  return noteLevelTrackerRowLabels[kind]
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
