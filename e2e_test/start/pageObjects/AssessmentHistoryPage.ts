export const expectAssessmentHistory = () => {
  cy.findByText("Assessment History")
  return {}
}

export const navigateToAssessmentHistory = () => {
  cy.visit("/assessment-history")
  return expectAssessmentHistory()
}
