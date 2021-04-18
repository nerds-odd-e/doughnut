import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Then(
  "On day {int} I should have {string} note for initial review and {string} for repeat",
  (day, numberOfInitialReviews, numberOfRepeats) => {
    cy.timeTravelTo(day, 8);
    cy.visit("/reviews");
    cy.findByText(numberOfInitialReviews, {
      selector: ".number-of-initial-reviews"
    });
    cy.findByText(numberOfRepeats, { selector: ".number-of-repeats" });
  }
);

Then("I should see the option {string} is {string}", (option, status) => {
  const elm = cy.getFormControl(option);
  if (status === "on") {
    elm.should("be.checked");
  } else {
    elm.should("not.be.checked");
  }
});

Then("I should see the information of note {string}", noteTitle => {
  cy.findByText(noteTitle);
});
