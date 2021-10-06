/// <reference types="cypress" />
/// <reference types="@testing-library/cypress" />
/// <reference types="../../support" />
// @ts-check

import {
  And,
  Before,
  Given,
  Then,
  When,
} from "cypress-cucumber-preprocessor/steps";

When(
  "I create a new circle {string} and copy the invitation code",
  (circleName) => {
    cy.visit("/circles");
    cy.findByRole("button", {name: "Create a new circle"}).click();
    cy.getFormControl("Name").type(circleName);
    cy.get('input[value="Submit"]').click();

    cy.get("#invitation-code")
      .invoke("val")
      .then((text) => {
        cy.wrap(text).as("savedInvitationCode");
      });
  }
);

When("I visit the invitation link", () => {
  cy.get("@savedInvitationCode").then((invitationCode) =>
    cy.visit(invitationCode)
  );
});

When("I join the circle", () => {
  cy.get('input[value="Join"]').click();
});

When(
  "I should see the circle {string} and it has two members in it",
  (circleName) => {
    cy.navigateToCircle(circleName);
    cy.get("body").find(".circle-member").should("have.length", 2);
  }
);

Given(
  "There is a circle {string} with {string} members",
  (circleName, members) => {
    cy.seedCircle({circleName: circleName, members: members})
  }
);

When("I create a note {string} in circle {string}", (noteTitle, circleName) => {
  cy.navigateToCircle(circleName);
  cy.findByText("Add New Notebook In This Circle").click();
  cy.submitNoteFormsWith([{Title: noteTitle}]);
});

When(
  "I should see the note {string} in circle {string}",
  (noteTitle, circleName) => {
    cy.navigateToCircle(circleName);
    cy.findByText(noteTitle).should("be.visible");
  }
);

When("I add a note {string} under {string}", (noteTitle, parentNoteTitle) => {
  cy.findByText(parentNoteTitle).click();
  cy.clickAddChildNoteButton();
  cy.submitNoteFormsWith([{Title: noteTitle}]);
  cy.findByText(noteTitle).should("be.visible");
});

When(
  "I subscribe to note {string} in the circle {string}, with target of learning {int} notes per day",
  (noteTitle, circleName, count) => {
    cy.navigateToCircle(circleName);
    cy.subscribeToNote(noteTitle, count);
  }
);

Given("I've logged in as an existing user", () => {
  cy.loginAs("old_learner");
});

Given("I navigate to an existing circle {string} where the {string} and {string} users belong to",
  (circleName, user1, user2) => {
    cy.seedCircle({circleName: `${circleName}`, members: `${user1},${user2}`})
    cy.navigateToCircle(circleName);
  });

Given("a notebook already exists in the circle",
  () => {
    cy.createNotebook("Test notebook", "Notebook description");
  });
