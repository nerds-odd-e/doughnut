import { systemSidebar } from './pageObjects/systemSidebar'

export const logins = {
  goToAdminDashboard: () => {
    cy.reload()
    return systemSidebar().adminDashboard()
  },

  loginAsAdmin: () => {
    cy.logout()
    cy.loginAs('admin')
  },
  loginAsAdminAndGoToAdminDashboard() {
    this.loginAsAdmin()
    return this.goToAdminDashboard()
  },
}
