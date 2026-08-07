import { clickDaisyDialogButton } from '../../../support/daisyModalHelpers'
import { commonSenseSplit } from '../../../support/string_util'
import { waitUntilAppIsNotBusy } from '../../pageBase'
import { submittableForm } from '../../forms'

const ADMIN_DASHBOARD_TAB_QUERY: Record<string, string> = {
  'Failure Reports': 'failureReport',
  'Manage Models': 'manageModel',
  'Manage Bazaar': 'manageBazaar',
  Users: 'users',
}

function removeNotebookFromBazaarTableRow(notebook: string) {
  cy.findByText(notebook)
    .parentsUntil('tr')
    .parent()
    .findByRole('button', { name: 'Remove' })
    .click()
  cy.findByRole('button', { name: 'OK' }).click()
  waitUntilAppIsNotBusy()
}

function modelManagement() {
  return {
    chooseModel(model: string, task: string) {
      submittableForm.submitWith({ [task]: model })
      return this
    },
  }
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

    openFailureReports() {
      cy.on('uncaught:exception', () => false)
      const tab = ADMIN_DASHBOARD_TAB_QUERY['Failure Reports']
      cy.visit(`/admin-dashboard?tab=${tab}`)
      waitUntilAppIsNotBusy()
      return this
    },

    expectFailureReportsAccessOutcome(outcome: string) {
      switch (outcome) {
        case 'sign in':
          cy.contains('Please sign in').should('be.visible')
          break
        case 'failure reports':
          cy.get('h2').should('contain', 'Failure Reports')
          break
        case 'access denied':
          cy.findByText('It seems you cannot access this page.').should(
            'be.visible'
          )
          break
        default:
          throw new Error(
            `Unknown failure reports access outcome: "${outcome}"`
          )
      }
      return this
    },

    openAdminDashboardTab(tabName: string) {
      const tab = ADMIN_DASHBOARD_TAB_QUERY[tabName]
      if (!tab) {
        throw new Error(`Unknown admin dashboard tab: ${tabName}`)
      }
      cy.visit(`/admin-dashboard?tab=${tab}`)
      cy.location('search').should('include', `tab=${tab}`)
      waitUntilAppIsNotBusy()
      return this
    },

    openBazaarAdminList() {
      return this.openAdminDashboardTab('Manage Bazaar')
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
      waitUntilAppIsNotBusy()
      return this
    },

    goToModelManagement() {
      this.openAdminDashboardTab('Manage Models')
      return modelManagement()
    },

    expectBazaarAdminNotebooks(notebooks: string) {
      waitUntilAppIsNotBusy()
      cy.location('pathname').should('include', 'admin-dashboard')
      cy.location('search').should('include', 'tab=manageBazaar')
      const expected = commonSenseSplit(notebooks, ',')
      cy.get('[data-testid="manage-bazaar-table"] tbody tr', {
        timeout: 15000,
      }).should(($rows) => {
        expect(
          $rows.length,
          `Expected bazaar admin list to have ${expected.length} notebook(s) [${expected.join(', ')}], but found ${$rows.length}`
        ).to.equal(expected.length)
      })
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
