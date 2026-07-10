import { pageIsNotLoading } from '../pageBase'
import { mainMenu } from '../pageObjects/mainMenu'
import { HealthCheckController } from '@generated/doughnut-backend-api/sdk.gen'

export const loginActions = {
  logout() {
    pageIsNotLoading()
    cy.wrap(null).as('currentLoginUser')
    return cy
      .request({
        method: 'POST',
        url: '/logout',
      })
      .its('status')
      .should('eq', 204)
      .clearAllCookies()
  },

  establishSessionAs(username: string) {
    cy.wrap(username).as('currentLoginUser')
    cy.wrap(username).as('injectNotesExternalIdentifier')
    return cy.wrap(
      HealthCheckController.ping({
        headers: {
          Authorization: `Basic ${btoa(`${username}:password`)}`,
        },
      })
    )
  },

  loginAs(username: string) {
    if (username === 'none') {
      return this.logout()
    }

    return this.establishSessionAs(username).then(() => {
      cy.visit('/notebooks')
    })
  },

  reloginAs(username: string) {
    return this.logout().then(() => {
      return this.loginAs(username)
    })
  },

  reloginAndEnsureHomePage(username: string) {
    const displayName = username
      .split('_')
      .map((word) => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase())
      .join(' ')
    return this.reloginAs(username).then(() => {
      cy.visit('/')
      mainMenu().userOptions().userSettingsButton(displayName)
    })
  },

  loginAsAdmin() {
    return this.loginAs('admin')
  },

  reloginAsAdmin() {
    return this.logout().then(() => {
      return this.loginAsAdmin()
    })
  },

  goToAdminDashboard() {
    cy.reload()
    return mainMenu().adminDashboard()
  },

  loginAsAdminAndGoToAdminDashboard() {
    this.reloginAsAdmin()
    return this.goToAdminDashboard()
  },
}
