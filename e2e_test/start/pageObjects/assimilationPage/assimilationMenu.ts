import { UserController } from '@generated/doughnut-backend-api/sdk.gen'
import type { MenuDataDto } from '@generated/doughnut-backend-api'
import { pageIsNotLoading } from '../../pageBase'
import {
  assimilationDueFromTriple,
  assimilationToastMessages,
  expectSuccessToast,
} from './shared'

export const assimilation = () => {
  const getAssimilateListItemInSidebar = (
    fn: ($el: Cypress.Chainable<JQuery<HTMLElement>>) => void
  ) => cy.get('.main-menu').within(() => fn(cy.get('li[title="Assimilate"]')))

  return {
    expectCount(numberOfNotes: number) {
      getAssimilateListItemInSidebar(($el) => {
        $el.findByText(`${numberOfNotes}`, { selector: '.due-count' })
      })
      return this
    },
    expectAssimilationDueFromTriple(toAssimilateAndTotal: string) {
      const expectedDue = assimilationDueFromTriple(toAssimilateAndTotal)
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone
      cy.wrap(UserController.getMenuData({ query: { timezone } }), {
        log: false,
      }).then((menuData: MenuDataDto) => {
        expect(menuData.assimilationCount?.dueCount ?? 0).to.eq(expectedDue)
      })
      return this
    },
    startAssimilationFromMenu() {
      getAssimilateListItemInSidebar(($el) => {
        $el.click()
      })
      pageIsNotLoading()
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
