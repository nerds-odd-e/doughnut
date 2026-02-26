/// <reference types="cypress" />
// @ts-check

type RouteParams = Record<string, string | number>

type CustomWindow = Omit<Cypress.AUTWindow, 'Infinity' | 'NaN'> & {
  Infinity: number
  NaN: number
  router?: {
    push: (options: {
      name: string
      params: RouteParams
      query: Record<string, string | number>
    }) => Promise<unknown>
  }
}

const router = () => {
  const push = (fallback: string, name: string, params: RouteParams) => {
    cy.get('@firstVisited').then((firstVisited) => {
      const isFirstVisited =
        (firstVisited as unknown as { valueOf(): string }).valueOf() === 'yes'
      cy.window().then((win: CustomWindow) => {
        if (win.router && isFirstVisited) {
          cy.wrap(
            win.router
              .push({
                name,
                params,
                query: { time: Date.now() },
              })
              .catch((error) => {
                cy.log('router push failed')
                cy.log(error as string)
                throw error
              })
          )
        } else {
          cy.wrap('yes').as('firstVisited')
          cy.visit(fallback)
        }
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
