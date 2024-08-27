import { routerToNotebooksPage } from './notebooksPage'
import { assumeAdminDashboardPage } from './adminPages/adminDashboardPage'
import { assumeAssessmentAndCertificateHistoryPage } from './assessmentAndCertificateHistoryPage'

export const systemSidebar = () => {
  routerToNotebooksPage()
  cy.pageIsNotLoading()
  cy.findByRole('button', { name: 'open sidebar' }).click({ force: true })

  return {
    adminDashboard() {
      cy.findByText('Admin Dashboard').click()
      return assumeAdminDashboardPage()
    },
    userOptions() {
      cy.findByRole('button', { name: 'User actions' }).click()
      return {
        userSettings(userName: string) {
          cy.findByRole('button', { name: `Settings for ${userName}` }).click()
          return {
            aiQuestionOnlyForReview() {
              cy.formField('Ai Question Type Only For Review').check()
              cy.findByText('Submit').click()
            },
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
        myFeedbackOverview() {
          cy.findByRole('button', {
            name: 'Feedback overview',
          }).click()
        }
      }
    },
  }
}
