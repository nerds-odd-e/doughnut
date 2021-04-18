import {
    Given,
    And,
    Then,
    When,
    Before
} from "cypress-cucumber-preprocessor/steps";

Then(
    "I should be asked link question {string} {string} with options {string}",
    (noteTitle, linkType, options) => {
        cy.shouldSeeQuizWithOptions([noteTitle, linkType], options);
    }
);