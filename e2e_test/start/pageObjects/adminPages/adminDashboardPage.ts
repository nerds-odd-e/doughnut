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

    goToTabInAdminDashboard(tabName:string) {
      cy.findByText(tabName).click()

      switch (tabName) {
        case "Failure Reports":
          cy.findByText("Failure report list")
          break
        case "Manage Model":
          cy.get("select[name='Question Generation']").should('contains.text', '---');
          break
      }

      return assumeAdminDashboardPage()
    },

    chooseModelNameInEngine(modelName:string, trainingEngine:string) {
      cy.get("select[name='" + trainingEngine + "']").select(modelName)
      cy.get(".saveBtn").click()
      return {}
    },

    assumeAdminCanSeeModelOption(modelName:string, trainingEngine:string) {
      cy.get("select[name='" + trainingEngine + "']").select(modelName)
    },

    assumeSelectionWithDefaultOption(modelName:string, trainingEngine:string) {
      cy.get("select[name='" + modelName + "']")
        .find("option:selected")
        .should("have.text", trainingEngine)
    }
  }
}
