import {
    Given,
    And,
    Then,
    When,
    Before
} from "cypress-cucumber-preprocessor/steps";

Then(
    "I should be asked picture question {string} with options {string}",
    (pictureInQuestion, options) => {
        cy.shouldSeeQuizWithOptions([], options);
    }
);

Then("The randomizer always choose the last", yesNo => {
    cy.randomizerAlwaysChooseLast();
});

Then(
    "I should be asked picture selection question {string} with {string}",
    (question, options) => {
        cy.shouldSeeQuizWithOptions([question], "");
    }
);