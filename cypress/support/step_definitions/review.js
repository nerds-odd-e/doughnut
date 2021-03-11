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
    cy.visit('/reviews/initial');
    const [review_type, title] = ["single note", noteTitle];
    cy.initialReviewOneNoteIfThereIs({review_type, title});
});





