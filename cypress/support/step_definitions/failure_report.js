import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I've logged in as {string}", (externalIdentifier) => {
  cy.loginAs(externalIdentifier);
});

When("I open the {string} set address bar", (url) => {
  cy.visit('http://localhost:8081/' + url)
});

Then("there shouldn't be any note edit button for {string}", (noteTitle) => {
  cy.findNoteCardEditButton(noteTitle).should("not.exist");
});

When("I open the detail {string} in the top bar", (noteTitle) => {
  cy.findByText(noteTitle).click();
});



Given("I've logged in as an existing user", () => {
  cy.loginAs('old_learner');
});

Given("I've logged in as another existing user", () => {
  cy.loginAs('another_old_learner');
});

When("I identify myself as a new user", () => {
  cy.visit("/login");

  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get("form.form-signin").submit();
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
});

When("I should be asked to create my profile", () => {
  cy.get("body").should("contain", "Please create your profile");
});

When("I save my profile with:", (data) => {
  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      cy.getFormControl(propName).type(elem[propName]);
    }
  });
  cy.get('input[value="Submit"]').click();
});

Then("I should see {string} in the page", (content) => {
  cy.get("body").should("contain", content);
});

Then("My name {string} is in the top bar", (name) => {
  cy.get("nav").should("contain", name);
});

Then("my daily new notes to review is set to {int}", (number) => {
  cy.updateCurrentUserSettingsWith({ daily_new_notes_count: number });
});

Then("my space setting is {string}", (number) => {
  cy.updateCurrentUserSettingsWith({ space_intervals: number });
});

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
