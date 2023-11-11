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

    suggestedQuestionsForFineTuning() {
      this.goToTabInAdminDashboard("Fine Tuning Data")
      return adminFineTuningPage()
    },

    goToTabInAdminDashboard(tabName: string) {
      cy.findByRole("button", { name: tabName }).click()
      return this
    },

    chooseModelNameInEngine(modelName: string, trainingEngine: string) {
      cy.get("select[name='" + trainingEngine + "']").select(modelName)
      cy.get(".saveBtn").click()
      return {}
    },

    assumeAdminCanSeeModelOption(modelName: string, trainingEngine: string) {
      cy.get("select[name='" + trainingEngine + "']").select(modelName)
    },

    assumeSelectionWithDefaultOption(modelName: string, trainingEngine: string) {
      cy.get("select[name='" + modelName + "']")
        .find("option:selected")
        .should("have.text", trainingEngine)
    },
  }
}
