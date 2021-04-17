import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

When("Someone triggered an exception", (url) => {
  cy.triggerException();
});

Then("I should see {string} in the failure report", (content) => {
  cy.visit("/failure-report-list");
  cy.get("body").should("contain", content);
});

When("I should see a new open issue on github", () => {
  cy.request({method: "GET", url: `/api/testability/github_issues`}).then(response=>{
    expect(response.body.length).to.equal(1);
  });
});

Given("There are no open issues on github", (id) => {
  cy.request({method: "POST", url: `/api/testability/close_all_github_issues`})
     .its("body").should("contain", "OK");
});
