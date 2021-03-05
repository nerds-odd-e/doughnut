import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("I do these initial reviews in sequence:", (data) => {
  cy.visit('/reviews/initial');
  data.hashes().forEach(initialReview => {
    cy.initialReviewOneNoteIfThereIs(initialReview);
  });
})

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

Then("On day {int} at {int} hour I repeat old {string} and at {int} hour initial review new {string}", (day, repeatHour, repeatNotes, initialHour, initialNotes) => {
    cy.timeTravelTo(day, repeatHour);
    cy.visit('/reviews/repeat');
    repeatNotes.commonSenseSplit(",").forEach(title => {
        const review_type = "single note";
        cy.repeatReviewOneNoteIfThereIs({review_type, title});
    });

    cy.timeTravelTo(day, initialHour);
    cy.visit('/reviews/initial');
    initialNotes.commonSenseSplit(", ").forEach(title => {
        const review_type = "single note";
        cy.initialReviewOneNoteIfThereIs({review_type, title});
    });

});

