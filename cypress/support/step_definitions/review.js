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

Then("On day {int} I repeat old {string} and initial review new {string}", (day, repeatNotes, initialNotes) => {
    cy.timeTravelTo(day, 8);
    cy.visit('/reviews/repeat');
    repeatNotes.commonSenseSplit(",").forEach(title => {
        const review_type = title === "end" ? "repeat done" : "single note";
        cy.repeatReviewOneNoteIfThereIs({review_type, title});
    });

    cy.visit('/reviews/initial');
    initialNotes.commonSenseSplit(", ").forEach(title => {
        const review_type = title === "end" ? "initial done" : "single note";
        cy.initialReviewOneNoteIfThereIs({review_type, title});
    });

});

