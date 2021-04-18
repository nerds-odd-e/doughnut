import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When("I visit {string} page", pageName => {
  switch (pageName) {
    case "FailureReportPage":
      cy.visit("/failure-report-list", {
        failOnStatusCode: false
      });
      break;
    default:
      cy.failure();
  }
});

Then("The {string} page is displayed", pageName => {
  switch (pageName) {
    case "LoginPage":
      cy.findAllByText("Please sign in");
      break;
    case "FailureReportPage":
      cy.findAllByText("Failure report list");
      break;
    case "ErrorPage":
      cy.findAllByText("Whitelabel Error Page");
      break;
    default:
      cy.failure();
  }
});
