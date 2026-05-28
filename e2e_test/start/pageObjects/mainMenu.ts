import { pageIsNotLoading } from '../pageBase'
import { form } from '../forms'
import { navigateToNotebooksPage } from './myNotebooksPage'
import { assumeAdminDashboardPage } from './adminPages/adminDashboardPage'
import { messageCenterIndicator } from './messageCenterIndicator'
import { manageAccessTokensPage } from './manageAccessTokensPage'

export const mainMenu = () => {
  navigateToNotebooksPage()
  pageIsNotLoading()

  return {
    adminDashboard() {
      return this.userOptions().adminDashboard()
    },
    userOptions() {
      cy.findByRole('button', { name: 'Account' }).click()
      return {
        adminDashboard() {
          cy.findByText('Admin Dashboard').click({ force: true })
          return assumeAdminDashboardPage()
        },

        userSettingsButton(userName: string) {
          return cy.findByRole('button', { name: `Settings for ${userName}` })
        },

        userSettings(userName: string) {
          this.userSettingsButton(userName).click()
          return {
            changeName(name: string) {
              form.getField('Name').assignValue(name)
              cy.findByText('Submit').click()
              pageIsNotLoading()
            },
          }
        },

        logout() {
          cy.findByRole('button', { name: 'Logout' }).click({ force: true })
        },
        manageAccessTokens() {
          cy.findByRole('link', { name: 'Manage Access Tokens' }).click({
            force: true,
          })
          return manageAccessTokensPage()
        },
      }
    },
    myMessageCenter() {
      return messageCenterIndicator().go()
    },
  }
}
