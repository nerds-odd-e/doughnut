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

  const whenAppIsOpen = (whenOpen: () => unknown, whenNotOpen: () => unknown) =>
    cy
      .get('@firstVisited')
      .then((firstVisited) =>
        (firstVisited as unknown as { valueOf(): string }).valueOf() === 'yes'
          ? whenOpen()
          : whenNotOpen()
      )

  /** Loads the app at the notebooks page unless this scenario already opened it. */
  const openApp = () =>
    whenAppIsOpen(
      () => undefined,
      () => visitNamed('notebooks')
    )

  /** Reloads an app this scenario already opened, e.g. so it shows a newly logged-in user. */
  const reloadOpenApp = () =>
    whenAppIsOpen(
      () => visitNamed('notebooks'),
      () => undefined
    )

  const push = (name: string, params: RouteParams = {}, query?: RouteQuery) =>
    whenAppIsOpen(
      () =>
        cy.window().then((win: CustomWindow) => {
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
        }),
      () => visitNamed(name, params, query)
    )

  return {
    openApp,
    push,
    reloadOpenApp,
    visitNamed,
  }
}

export default router
