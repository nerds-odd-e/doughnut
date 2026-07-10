import { pageIsNotLoading } from '../../pageBase'

export const assimilateButtonSelector = '[data-test="assimilate"]'
export const reviveButtonSelector = '[data-test="revive"]'
export const skipRecallButtonSelector = '[value="Skip recall"]'

export const assimilationPropertyRow = (propertyKey: string) =>
  cy.get(
    `[data-test="assimilation-property-row"][data-property-key="${propertyKey}"]`
  )

export const isNoteLevelAssimilationControl = (el: Element) =>
  el.closest('[data-test="assimilation-property-row"]') === null

export const assimilateButton = (options?: { timeout?: number }) =>
  cy
    .get(assimilateButtonSelector, options ?? {})
    .filter((_, el) => isNoteLevelAssimilationControl(el))

export const reviveButton = (options?: { timeout?: number }) =>
  cy
    .get(reviveButtonSelector, options ?? {})
    .filter((_, el) => isNoteLevelAssimilationControl(el))

export const skipRecallOnPanel = (options?: { timeout?: number }) =>
  cy
    .get(skipRecallButtonSelector, options ?? {})
    .filter((_, el) => isNoteLevelAssimilationControl(el))

export function noteLevelReviveElements(doc: Document | ParentNode): Element[] {
  return [...doc.querySelectorAll(reviveButtonSelector)].filter(
    (el) => !el.closest('[data-test="assimilation-property-row"]')
  )
}

export const refinementLayoutPanel = () =>
  cy
    .get('[data-test="refine-note-modal"]')
    .contains('Note layout:')
    .closest('.bg-accent')

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

export function waitForAssimilationNoteTitle(expectedTitle?: string) {
  pageIsNotLoading()
  cy.get('#main-note-content', { timeout: 15000 }).should('be.visible')
  const title = cy.get(mainNoteHeadingTitleSelector, { timeout: 15000 })
  if (expectedTitle !== undefined && expectedTitle.trim() !== '') {
    title.should('contain', expectedTitle.trim())
  } else {
    title.should('exist')
  }
}
