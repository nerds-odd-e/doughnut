import { pageIsNotLoading } from '../../pageBase'

export const keepForRecallButton = (options?: { timeout?: number }) =>
  cy.get('[data-test="keep-for-recall"]', options ?? {})

export const refinementSuggestionsPanel = () =>
  cy
    .get('[data-test="refine-note-modal"]')
    .contains('Refinement suggestions:')
    .closest('.bg-accent')

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
