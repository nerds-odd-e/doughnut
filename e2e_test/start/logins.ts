import start from './'
import { systemSidebar } from './pageObjects/systemSidebar'

export const logins = {
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
    this.logout()

    if (username === 'none') {
      return
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

  goToAdminDashboard: () => {
    cy.reload()
    return systemSidebar().adminDashboard()
  },

  loginAsAdmin() {
    this.logout().loginAs('admin')
  },
}
