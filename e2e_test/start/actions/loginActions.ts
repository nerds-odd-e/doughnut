import { mainMenu } from '../pageObjects/mainMenu'
import start from '../index'
import * as Services from '@generated/backend/sdk.gen'
import { OpenAPI } from '@generated/backend/core/OpenAPI'

export const loginActions = {
  logout() {
    cy.pageIsNotLoading()
    cy.request({
      method: 'POST',
      url: '/logout',
    }).then((response) => {
      expect(response.status).to.equal(204)
    })
    cy.pageIsNotLoading()
    return this
  },

  loginAs(username: string) {
    if (username === 'none') {
      this.logout()
      return start
    }

    const password = 'password'

    // Set Basic auth in OpenAPI config
    const originalUsername = OpenAPI.USERNAME
    const originalPassword = OpenAPI.PASSWORD
    OpenAPI.USERNAME = username
    OpenAPI.PASSWORD = password

    // Call the service directly - it will use cy.request via our custom request function
    return Services.ping()
      .then(() => {
        // Success
      })
      .finally(() => {
        // Restore original credentials
        OpenAPI.USERNAME = originalUsername
        OpenAPI.PASSWORD = originalPassword
      })
      .then(() => start)
  },

  reloginAs(username: string) {
    return this.logout().loginAs(username)
  },

  reloginAndEnsureHomePage(username: string) {
    const loginPromise = this.reloginAs(username)
    cy.visit('/')
    return loginPromise
  },

  loginAsAdmin() {
    return this.loginAs('admin')
  },

  reloginAsAdmin() {
    return this.logout().loginAsAdmin()
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
