import { adminFineTuningPage } from "./adminFineTuningPage"

export function assumeAdminDashboardPage() {
  return {
    goToFailureReportList() {
      this.goToTabInAdminDashboard("Failure Reports")
      cy.findByText("Failure report list")
      return {
        shouldContain(content: string) {
          cy.get("body").should("contain", content)
        },
      }
    },

    goToFineTuningData() {
      this.goToTabInAdminDashboard("Fine Tuning Data")
      return adminFineTuningPage()
    },

    goToTabInAdminDashboard(tabName: string) {
      cy.findByRole("button", { name: tabName }).click()
    },
  }
}
