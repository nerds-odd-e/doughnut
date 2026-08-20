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
      cy.intercept('POST', '**/api/settings/current-model-version').as(
        'saveModelVersions'
      )
      submittableForm.fill({ [task]: model })
      cy.findByRole('button', { name: 'Save' }).click()
      cy.wait('@saveModelVersions').its('response.statusCode').should('eq', 200)
      return this
    },
  }
}

function failureReportList() {
  cy.get('h2').contains('Failure Reports')
  return {
    shouldContain(content: string) {
      cy.get('body').should(($body) => {
        const actual = $body.text()
        expect(
          actual,
          `Expected failure report to contain "${content}", but found: ${actual.slice(0, 500)}`
        ).to.contain(content)
      })
      return this
    },
    clearSelected(index = 0) {
      cy.get('.daisy-card').eq(index).find('input[type="checkbox"]').check()
      cy.get('button').contains('Delete Selected').click()
      clickDaisyDialogButton('dialog.daisy-modal', 'Delete')
      waitUntilAppIsNotBusy()
      return this
    },
    shouldBeEmpty() {
      cy.findByText('All Clear!').should('exist')
      return this
    },
  }
}

export function assumeAdminDashboardPage() {
  return {
    goToFailureReportList() {
      this.goToTabInAdminDashboard('Failure Reports')
      return failureReportList()
    },

    assumeFailureReportList() {
      return failureReportList()
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
      cy.intercept('GET', '**/api/ai/available-gpt-models').as(
        'availableGptModels'
      )
      cy.intercept('GET', '**/api/settings/current-model-version').as(
        'currentModelVersions'
      )
      const tab = ADMIN_DASHBOARD_TAB_QUERY['Manage Models']
      cy.visit(`/admin-dashboard?tab=${tab}`)
      cy.wait(['@availableGptModels', '@currentModelVersions'])
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
