import { adminFineTuningPage } from "./adminFineTuningPage"

export function assumeAdminDashboardPage() {
  return {
    goToFailureReportList() {
      cy.findByText("Failure Reports").click()
      return {
        shouldContain(content: string) {
          cy.get("body").should("contain", content)
        },
      }
    },

    suggestedQuestionsForFineTuning() {
      cy.findByRole("button", { name: "Fine Tuning Data" }).click()
      return adminFineTuningPage()
    },

    goToModelManagementTab(tabName: string) {
      cy.findByText(tabName).click()
      return {}
    },
  }
}
