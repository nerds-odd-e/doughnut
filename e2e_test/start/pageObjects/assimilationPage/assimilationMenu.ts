import { UserController } from '@generated/donut-backend-api/sdk.gen'
import type { MenuDataDto } from '@generated/donut-backend-api'
import { waitUntilAppIsNotBusy, withinMainMenuItem } from '../../pageBase'
import {
  assimilationDueFromTriple,
  assimilationToastMessages,
  expectSuccessToast,
} from './shared'

export const assimilation = () => {
  const getAssimilateListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => withinMainMenuItem('Assimilate', fn)

  const clickAssimilateListItemInSidebar = () => {
    getAssimilateListItemInSidebar(($el) => {
      $el.click()
    })
  }

  return {
    expectAssimilationNavBadge(dueOverTotal: string) {
      getAssimilateListItemInSidebar(($el) => {
        $el.findByText(dueOverTotal, { selector: '.due-count' })
      })
      return this
    },
    expectAssimilationDueFromTriple(toAssimilateAndTotal: string) {
      const expectedDue = assimilationDueFromTriple(toAssimilateAndTotal)
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      cy.then(() => UserController.getMenuData({ query: { timezone } })).then(
        (menuData: MenuDataDto) => {
          expect(
            menuData.assimilationCount?.dueCount ?? 0,
            `assimilation due for ${toAssimilateAndTotal}`
          ).to.eq(expectedDue)
        }
      )
      return this
    },
    startAssimilationFromMenu() {
      clickAssimilateListItemInSidebar()
      waitUntilAppIsNotBusy()
      return this
    },
    startAssimilationFromMenuWhileNextNoteLoadsSlowly() {
      cy.intercept(
        { method: 'GET', url: '/api/assimilation/next*', times: 1 },
        (req) => {
          req.continue((res) => {
            res.setDelay(300)
          })
        }
      ).as('nextAssimilation')

      clickAssimilateListItemInSidebar()
      cy.get('.loading-modal-mask', { timeout: 10000 }).should('be.visible')
      cy.contains('p.loading-message', 'Loading next note...').should(
        'be.visible'
      )
      cy.wait('@nextAssimilation')
      waitUntilAppIsNotBusy()
      return this
    },
    expectDailyAssimilationGoalToast() {
      expectSuccessToast(assimilationToastMessages.dailyGoalMet)
      return this
    },
    expectNoMoreNotesToAssimilateToast() {
      expectSuccessToast(assimilationToastMessages.noMoreNotes)
      return this
    },
    expectAssimilationMenuProgress() {
      getAssimilateListItemInSidebar(($el) => {
        $el
          .find('[data-testid="assimilation-menu-progress"]')
          .should('be.visible')
      })
      return this
    },
  }
}
