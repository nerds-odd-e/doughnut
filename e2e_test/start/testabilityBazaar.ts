/// <reference types="Cypress" />
// @ts-check
import type { BazaarNotebook } from '@generated/donut-backend-api'
import {
  BazaarController,
  SubscriptionController,
  TestabilityRestController,
} from '@generated/donut-backend-api/sdk.gen'
import { unwrapData } from './unwrapApi'

export const bazaarTestabilityMethods = {
  shareToBazaar(notebookName: string) {
    return cy.wrap(
      TestabilityRestController.shareToBazaar({
        body: { notebookName },
      }),
      { log: false }
    )
  },

  subscribeToBazaarNotebook(
    notebookName: string,
    dailyTargetOfNewNotes: number
  ) {
    return cy
      .wrap(BazaarController.bazaar(), { log: false })
      .then((response) => {
        const bazaarNotebooks = unwrapData<BazaarNotebook[]>(response)
        const match = bazaarNotebooks.find(
          (item) => item.notebook.name === notebookName
        )
        expect(match, `bazaar notebook "${notebookName}" was not found`).to
          .exist
        return cy.wrap(
          SubscriptionController.createSubscription({
            path: { notebook: match!.notebook.id },
            body: { dailyTargetOfNewNotes },
          }),
          { log: false }
        )
      })
  },
}
