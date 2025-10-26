import { mainMenu } from '../pageObjects/mainMenu'
import start from '../index'

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
    const token = btoa(`${username}:${password}`)
    cy.request({
      method: 'GET',
      url: '/api/healthcheck',
      headers: {
        Authorization: `Basic ${token}`,
      },
    }).then((response) => {
      expect(response.status).to.equal(200)
    })

    return start
  },

  reloginAs(username: string) {
    return this.logout().loginAs(username)
  },

  reloginAndEnsureHomePage(username: string) {
    const result = this.reloginAs(username)
    cy.visit('/')
    return result
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
