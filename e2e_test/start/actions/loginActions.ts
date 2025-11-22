import { mainMenu } from '../pageObjects/mainMenu'
import * as Services from '@generated/backend/sdk.gen'

export const loginActions = {
  logout() {
    cy.pageIsNotLoading()
    cy.wrap(null).as('currentLoginUser')
    return cy
      .request({
        method: 'POST',
        url: '/logout',
      })
      .then((response) => {
        expect(response.status).to.equal(204)
      })
  },

  loginAs(username: string) {
    if (username === 'none') {
      return this.logout()
    }

    // Call the service directly - it will use cy.request via our custom request function
    cy.wrap(username).as('currentLoginUser')
    return cy.wrap(
      Services.ping({
        headers: {
          Authorization: `Basic ${btoa(`${username}:password`)}`,
        },
      })
    )
  },

  reloginAs(username: string) {
    return this.logout().then(() => {
      return this.loginAs(username)
    })
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
