import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("I do these initial reviews in sequence:", (data) => {
  cy.visit('/review');
  data.hashes().forEach(initialReview => {
    cy.initialReviewOneNoteIfThereIs(initialReview);
  });
})

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

Then("I should be able to follow these review actions:", (data) => {
  data.hashes().forEach(({day, old_notes_to_review, initial_review}) => {
    cy.timeTravelTo(day, 8);
    cy.log(initial_review)
  });
});

