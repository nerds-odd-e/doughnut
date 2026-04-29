/// <reference types="cypress" />
// @ts-check

type RouteParams = Record<string, string | number>

type CustomWindow = Omit<Cypress.AUTWindow, 'Infinity' | 'NaN'> & {
  Infinity: number
  NaN: number
  router?: {
    push: (options: Record<string, unknown>) => Promise<unknown>
  }
}

const router = () => {
  const push = (fallback: string, name: string, params: RouteParams) => {
    cy.get('@firstVisited').then((firstVisited) => {
      const isFirstVisited =
        (firstVisited as unknown as { valueOf(): string }).valueOf() === 'yes'
      return cy.window().then((win: CustomWindow) => {
        if (win.router && isFirstVisited) {
          const noteId = params.noteId
          const isBasenameNoteShow =
            name === 'noteShow' &&
            typeof noteId === 'string' &&
            !/^\d+$/.test(noteId)
          const location = isBasenameNoteShow
            ? { path: fallback, query: { time: Date.now() } }
            : { name, params, query: { time: Date.now() } }
          return cy.wrap(
            win.router.push(location).catch((error) => {
              cy.log('router push failed')
              cy.log(error as string)
              throw error
            })
          )
        }
        cy.wrap('yes').as('firstVisited')
        return cy.visit(fallback)
      })
    })
  }

  return {
    push,
    toRoot() {
      push('/', 'root', {})
    },
  }
}

export default router
