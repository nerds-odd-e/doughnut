import { mainMenu } from '../pageObjects/mainMenu'
import start from '../index'
import * as Services from '@generated/backend/sdk.gen'

export const loginActions = {
  logout() {
    cy.pageIsNotLoading()
    cy.request({
      method: 'POST',
      url: '/logout',
    }).then((response) => {
      expect(response.status).to.equal(204)
    })
    cy.wrap(null).as('currentLoginUser')
    cy.pageIsNotLoading()
    return this
  },

  loginAs(username: string) {
    if (username === 'none') {
      this.logout()
      return start
    }

    // Call the service directly - it will use cy.request via our custom request function
    cy.wrap(username).as('currentLoginUser')
    return Services.ping({
      headers: {
        Authorization: `Basic ${btoa(`${username}:password`)}`,
      },
    } as Parameters<typeof Services.ping>[0])
      .then(() => {
        // Success
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
