import start from './'
import { systemSidebar } from './pageObjects/systemSidebar'

export const logins = {
  loginAs(username: string) {
    cy.logout()

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

  goToAdminDashboard: () => {
    cy.reload()
    return systemSidebar().adminDashboard()
  },

  loginAsAdmin() {
    cy.logout()
    this.loginAs('admin')
  },

  loginAsAdminAndGoToAdminDashboard() {
    this.loginAsAdmin()
    return this.goToAdminDashboard()
  },
}
