import { clickDaisyDialogButton } from '../../../support/daisyModalHelpers'
import { commonSenseSplit } from '../../../support/string_util'
import { pageIsNotLoading } from '../../pageBase'
import { submittableForm } from '../../forms'

const ADMIN_DASHBOARD_TAB_QUERY: Record<string, string> = {
  'Failure Reports': 'failureReport',
  'Manage Models': 'manageModel',
  'Manage Bazaar': 'manageBazaar',
  Users: 'users',
  'Data migration': 'dataMigration',
}

function removeNotebookFromBazaarTableRow(notebook: string) {
  cy.findByText(notebook)
    .parentsUntil('tr')
    .parent()
    .findByRole('button', { name: 'Remove' })
    .click()
  cy.findByRole('button', { name: 'OK' }).click()
  pageIsNotLoading()
}

export function assumeAdminDashboardPage() {
  return {
    goToFailureReportList() {
      this.goToTabInAdminDashboard('Failure Reports')
      cy.get('h2').contains('Failure Reports')
      return {
        shouldContain(content: string) {
          cy.get('body').should('contain', content)
        },
        checkFailureReportItem(index = 0) {
          cy.get('.daisy-card').eq(index).find('input[type="checkbox"]').check()
          return this
        },
        deleteSelected() {
          cy.get('button').contains('Delete Selected').click()
          clickDaisyDialogButton('dialog.daisy-modal', 'Delete')
          return this
        },
        shouldBeEmpty() {
          cy.findByText('All Clear!').should('exist')
          return this
        },
      }
    },

    openAdminDashboardTab(tabName: string) {
      const tab = ADMIN_DASHBOARD_TAB_QUERY[tabName]
      if (!tab) {
        throw new Error(`Unknown admin dashboard tab: ${tabName}`)
      }
      if (tab === 'manageBazaar') {
        cy.intercept('GET', '**/api/bazaar').as('bazaarAdminList')
      }
      cy.visit(`/admin-dashboard?tab=${tab}`)
      cy.location('search').should('include', `tab=${tab}`)
      if (tab === 'manageBazaar') {
        cy.wait('@bazaarAdminList').then((interception) => {
          expect(interception.response?.statusCode).to.eq(200)
          expect(interception.response?.body as unknown[]).to.not.be.empty
        })
      }
      pageIsNotLoading()
      return this
    },

    goToTabInAdminDashboard(tabName: string) {
      const tab = ADMIN_DASHBOARD_TAB_QUERY[tabName]
      if (!tab) {
        throw new Error(`Unknown admin dashboard tab: ${tabName}`)
      }
      cy.findByRole('button', { name: tabName }).click()
      cy.location('search').should('include', `tab=${tab}`)
      cy.findByRole('button', { name: tabName }).should(
        'have.class',
        'daisy-tab-active'
      )
      pageIsNotLoading()
      return this
    },

    goToModelManagement() {
      this.goToTabInAdminDashboard('Manage Models')
      return {
        chooseModel(model: string, task: string) {
          submittableForm.submitWith({ [task]: model })
        },
      }
    },

    expectBazaarAdminNotebooks(notebooks: string) {
      pageIsNotLoading()
      cy.location('pathname').should('include', 'admin-dashboard')
      cy.location('search').should('include', 'tab=manageBazaar')
      const expected = commonSenseSplit(notebooks, ',')
      cy.get('[data-testid="manage-bazaar-table"] tbody tr', {
        timeout: 15000,
      }).should('have.length', expected.length)
      for (const name of expected) {
        cy.get('[data-testid="manage-bazaar-table"] tbody tr')
          .contains('a', name)
          .should('be.visible')
      }
      return this
    },

    removeNotebookFromBazaarAdminList(notebook: string) {
      removeNotebookFromBazaarTableRow(notebook)
      return this
    },
  }
}
