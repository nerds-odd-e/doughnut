import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Then("my space setting is {string}", number => {
  cy.updateCurrentUserSettingsWith({ space_intervals: number });
});

Then("I initial review {string}", noteTitle => {
  cy.initialReviewNotes(noteTitle);
});
