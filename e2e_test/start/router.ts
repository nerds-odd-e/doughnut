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
          const stringParams = Object.fromEntries(
            Object.entries(params).map(([k, v]) => [k, String(v)])
          )
          const location =
            name.length > 0
              ? {
                  name,
                  params: stringParams,
                  query: { time: Date.now() },
                }
              : { path: fallback, query: { time: Date.now() } }
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
