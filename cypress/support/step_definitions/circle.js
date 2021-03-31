import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

When("I create a new circle {string} and copy the invitation code", (circleName) => {
  cy.visit("/circles/new");
  cy.getFormControl('name').type(circleName);
  cy.get('input[value="Submit"]').click();

  cy.get("#invitation-code").invoke('val').then((text)=>{
          cy.wrap(text).as("savedInvitationCode");
      });
});

When("I join the circle with the invitation code", () => {
  cy.visit("/circles");
  cy.get("@savedInvitationCode").then((invitationCode) => cy.getFormControl("invitationCode").type(invitationCode));
  cy.get('input[value="Submit"]').click();
});

When("I should see the circle {string} and it has two members in it", (circleName) => {
  cy.navigateToCircle(circleName);
  cy.get('body').find('.circle-member').should('have.length', 2);
});


Given("There is a circle {string} with {string} members", (circleName, members) => {
  cy.request({method: "POST", url: `/api/testability/seed_circle`,
   body: { circleName, members }})
  .then((response) => {
     expect(response.status).to.equal(200);
  })
});

When("I create a note {string} in circle {string}", (noteTitle, circleName) => {
  cy.navigateToCircle(circleName);
  cy.findByText("Add Top Level Note In This Circle").click();
  cy.submitNoteFormWith([{'noteContent.title': noteTitle}]);
});

When("I should see the note {string} in circle {string}", (noteTitle, circleName) => {
  cy.navigateToCircle(circleName);
  cy.findByText(noteTitle).should('be.visible');
});

When("I add a note {string} under {string}", (noteTitle, parentNoteTitle) => {
  cy.findByText(parentNoteTitle).click();
  cy.findByText("(Add Child Note)").click();
  cy.submitNoteFormWith([{'noteContent.title': noteTitle}]);
});



















