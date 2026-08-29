import { waitUntilAppIsNotBusy } from '../pageBase'
import { form } from '../forms'
import { navigateToNotebooksPage } from './notebooksPage'
import { assumeAdminDashboardPage } from './adminPages/adminDashboardPage'
import { messageCenterIndicator } from './messageCenterIndicator'
import { manageAccessTokensPage } from './manageAccessTokensPage'

const userSettingsForm = () => {
  const dailyProbeField = () => form.getField('Daily probe')
  return {
    submit() {
      cy.findByText('Submit').click()
      waitUntilAppIsNotBusy()
      return this
    },
    turnDailyProbeOn() {
      dailyProbeField().check()
      return this.submit()
    },
    turnDailyProbeOff() {
      dailyProbeField().uncheck()
      return this.submit()
    },
    reload() {
      cy.reload()
      waitUntilAppIsNotBusy()
      return this
    },
    expectDailyProbeOn() {
      dailyProbeField().shouldBeChecked()
      return this
    },
    expectDailyProbeOff() {
      dailyProbeField().shouldNotBeChecked()
      return this
    },
  }
}

export const assumeUserSettingsPage = () => userSettingsForm()

export const mainMenu = () => {
  navigateToNotebooksPage()

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
          return cy.findByRole('link', { name: `Settings for ${userName}` })
        },

        userSettings(userName: string) {
          this.userSettingsButton(userName).click()
          waitUntilAppIsNotBusy()
          return {
            changeName(name: string) {
              form.getField('Name').assignValue(name)
              return this.submit()
            },
            ...userSettingsForm(),
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
