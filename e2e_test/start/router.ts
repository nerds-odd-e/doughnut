/// <reference types="cypress" />
// @ts-check

import { namedLocationHref } from '@/routes/namedLocationHref'

type RouteParams = Record<string, string | number>
type RouteQuery = Record<string, string>

type CustomWindow = Omit<Cypress.AUTWindow, 'Infinity' | 'NaN'> & {
  Infinity: number
  NaN: number
  router?: {
    push: (options: Record<string, unknown>) => Promise<unknown>
  }
}

const stringifyValues = (obj: RouteParams) =>
  Object.fromEntries(Object.entries(obj).map(([k, v]) => [k, String(v)]))

const namedLocation = (name: string, params: RouteParams = {}) => ({
  name,
  params: stringifyValues(params),
})

const router = () => {
  const visitNamed = (
    name: string,
    params: RouteParams = {},
    query?: RouteQuery
  ) => {
    cy.wrap('yes').as('firstVisited')
    return cy.visit(
      namedLocationHref({ ...namedLocation(name, params), query })
    )
  }

  const push = (name: string, params: RouteParams = {}, query?: RouteQuery) => {
    cy.get('@firstVisited').then((firstVisited) => {
      const isFirstVisited =
        (firstVisited as unknown as { valueOf(): string }).valueOf() === 'yes'
      if (!isFirstVisited) {
        return visitNamed(name, params, query)
      }
      return cy.window().then((win: CustomWindow) => {
        if (!win.router) {
          return visitNamed(name, params, query)
        }
        return cy.wrap(
          win.router
            .push({ ...namedLocation(name, params), query })
            .catch((error) => {
              cy.log('router push failed')
              cy.log(error as string)
              throw error
            })
        )
      })
    })
  }

  return {
    push,
    visitNamed,
  }
}

export default router
