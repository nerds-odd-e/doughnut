import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("I do these initial reviews in sequence:", (data) => {
  cy.initialReviewInSequence(data.hashes());
})

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

Then("On day {int} I repeat old {string} and initial review new {string}", (day, repeatNotes, initialNotes) => {
    cy.timeTravelTo(day, 8);

    cy.repeatReviewNotes(repeatNotes);
    cy.initialReviewNotes(initialNotes);
});

Given("I go to the reviews page", () => {
    cy.visit("/reviews");
});

Then("I should see that I have old notes to repeat", () => {
  cy.findByRole('button', {name: "Start reviewing old notes"});
});

Then("I should see that I have new notes to learn", () => {
  cy.findByRole('button', {name: "Start reviewing new notes"});
});

Then("On day {int} I should have {string} note for initial review and {string} for repeat", (day, numberOfInitialReviews, numberOfRepeats) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews');
    cy.findByText(numberOfInitialReviews, {selector: '.number-of-initial-reviews'});
    cy.findByText(numberOfRepeats, {selector: '.number-of-repeats'});
});

Then("I initial review {string}", (noteTitle) => {
    cy.initialReviewNotes(noteTitle);
});

Then("I learned one note {string} on day {int}", (noteTitle, day) => {
    cy.seedNotes([{title: noteTitle}]);
    cy.timeTravelTo(day, 8);
    cy.initialReviewNotes(noteTitle);
});

Then("I repeat reviewing my old note on day {int}", (day) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews/repeat');
});

Then("choose to remove it fromm reviews", () => {
    cy.findByRole('button', {name: 'Remove This Note from Review'}).click();
});


