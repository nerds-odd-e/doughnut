import {
    Given,
    And,
    Then,
    When,
    Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I visit frontend app", () => {
    cy.visit("http://localhost:8000");
})

When("I should see the frontend app screenshot matches", () => {
    cy.get('flt-glass-pane').toMatchImageSnapshot({
        imageConfig: { threshold: 0.001, }
    });
})