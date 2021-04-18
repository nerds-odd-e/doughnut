import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

When(
  "I create a new circle {string} and copy the invitation code",
  circleName => {
    cy.visit("/circles/new");
    cy.getFormControl("Name").type(circleName);
    cy.get('input[value="Submit"]').click();

    cy.get("#invitation-code")
      .invoke("val")
      .then(text => {
        cy.wrap(text).as("savedInvitationCode");
      });
  }
);

When("I join the circle with the invitation code", () => {
  cy.visit("/circles");
  cy.get("@savedInvitationCode").then(invitationCode =>
    cy.getFormControl("InvitationCode").type(invitationCode)
  );
  cy.get('input[value="Submit"]').click();
});

When(
  "I should see the circle {string} and it has two members in it",
  circleName => {
    cy.navigateToCircle(circleName);
    cy.get("body")
      .find(".circle-member")
      .should("have.length", 2);
  }
);

When("I add a note {string} under {string}", (noteTitle, parentNoteTitle) => {
  cy.findByText(parentNoteTitle).click();
  cy.findByText("(Add Child Note)").click();
  cy.submitNoteFormWith([{ Title: noteTitle }]);
});
