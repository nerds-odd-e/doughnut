import { routerToMyNotebooksPage } from './myNotebooksPage'
import { assumeAdminDashboardPage } from './adminPages/adminDashboardPage'
import { assumeAssessmentAndCertificateHistoryPage } from './assessmentAndCertificateHistoryPage'
import { assumeMessageCenterPage } from './messageCenterPage'

export const systemSidebar = () => {
  routerToMyNotebooksPage()
  cy.pageIsNotLoading()
  cy.findByRole('button', { name: 'open sidebar' }).click({ force: true })

  return {
    adminDashboard() {
      return this.userOptions().adminDashboard()
    },
    userOptions() {
      cy.get('.modal-body').within(() => {
        cy.findByRole('button', { name: 'User actions' }).click()
      })
      return {
        adminDashboard() {
          cy.get('.modal-body').within(() => {
            cy.findByText('Admin Dashboard').click({ force: true })
          })
          return assumeAdminDashboardPage()
        },
        userSettings(userName: string) {
          cy.findByRole('button', { name: `Settings for ${userName}` }).click()
          return {
            changeName(name: string) {
              cy.formField('Name').assignFieldValue(name)
              cy.findByText('Submit').click()
            },
          }
        },
        logout() {
          cy.findByRole('button', { name: 'Logout' }).click()
        },
        myAssessmentAndCertificateHistory() {
          cy.findByRole('button', {
            name: 'My Assessments and Certificates',
          }).click()
          return assumeAssessmentAndCertificateHistoryPage()
        },
        myMessageCenter() {
          cy.findByRole('button', {
            name: 'Message center',
          }).click()

          return assumeMessageCenterPage()
        },
      }
    },
  }
}
