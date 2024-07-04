
export const navigateToAssessmentHistory = () => {
  cy.visit("/assessment-history")

  return expectAssessmentHistory()
}

export const expectAssessmentHistory = () => {
  cy.findByText("Assessment History")

  return {

  }
}
