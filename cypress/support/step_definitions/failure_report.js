import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("Login state is {string}", (loginState) => {
    switch(loginState) {
        case "None":
            break;
        case "Developer":
            cy.loginAs('developer');
            break;
        case "NonDeveloper":
            cy.loginAs('non_developer');
            break;
    }
});

When("Access to failure report page", () => {
  cy.visit("/failure-report-list");
});

Then("The {string} page is displayed", (pageName) => {
    switch(pageName) {
        case "LoginPage":
            cy.findAllByText("Please sign in");
            break;
        case "FailureReportPage":
            cy.findAllByText("Welcome To The Failure report list");
            break;
        case "ErrorPage":
            cy.findAllByText("Error");
            break;
        default:
            cy.failure();
    }
});

When("Access to top page", () => {
  cy.visit("/");
});

Then("Failure reports menu is {string} in the header", (visible) => {
    switch(visible) {
        case "Displayed":
            cy.findAllByText("Failure Reports");
            break;
        case "NotDisplayed":
            cy.findByText("Failure Reports").should('not.exist');
            break;
        default:
            cy.failure();
    }
});
