import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Then("I added and learned one note {string} on day {int}", (noteTitle, day) => {
  cy.seedNotes([{ title: noteTitle }]);
  cy.timeTravelTo(day, 8);
  cy.initialReviewNotes(noteTitle);
});

Then("choose to remove it from reviews", () => {
  cy.get("#more-action-for-repeat").click();
  cy.findByRole("button", { name: "Remove This Note from Review" }).click();
});

Then("I choose to do it again", () => {
  cy.get("#repeat-again").click();
});

Then("I should have {string} for repeat now", numberOfRepeats => {
  cy.visit("/reviews");
  cy.findByText(numberOfRepeats, { selector: ".number-of-repeats" });
});
