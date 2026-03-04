import { pageIsNotLoading } from '../../pageBase'
import { adminFineTuningPage } from './adminFineTuningPage'
import { submittableForm } from '../../forms'

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
          cy.get('.daisy-modal-action')
            .findByRole('button', { name: 'Delete' })
            .click()
          return this
        },
        shouldBeEmpty() {
          cy.findByText('All Clear!').should('exist')
          return this
        },
      }
    },

    goToTabInAdminDashboard(tabName: string) {
      cy.findByRole('button', { name: tabName }).click()
    },

    goToFineTuningData() {
      this.goToTabInAdminDashboard('Fine Tuning Data')
      return adminFineTuningPage()
    },

    goToModelManagement() {
      this.goToTabInAdminDashboard('Manage Models')
      return {
        chooseModel(model: string, task: string) {
          submittableForm.submitWith({ [task]: model })
        },
      }
    },

    goToBazaarManagement() {
      this.goToTabInAdminDashboard('Manage Bazaar')
      return {
        removeFromBazaar(notebook: string) {
          cy.findByText(notebook)
            .parentsUntil('tr')
            .parent()
            .findByRole('button', { name: 'Remove' })
            .click()
          cy.findByRole('button', { name: 'OK' }).click()
          pageIsNotLoading()
        },
      }
    },
    goToCertificationRequestPage() {
      this.goToTabInAdminDashboard('Certification Requests')
      return {
        approve(notebook: string) {
          cy.findByText(notebook)
            .parentsUntil('tr')
            .parent()
            .findByRole('button', { name: 'Approve' })
            .click()
          cy.findByRole('button', { name: 'OK' }).click()
        },
        listContainsExactly(notebooks: string[]) {
          cy.get('tbody tr').should('have.length', notebooks.length)
          notebooks.forEach((notebook) => {
            cy.findByText(notebook).should('exist')
          })
        },
      }
    },
  }
}
