import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I choose to share my notebook {string}", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, "Share notebook to bazaar").click();
  cy.findByRole('button', { name: 'OK' }).click();
})

Then("I should see {string} is shared in the Bazaar", (noteTitle) => {
  cy.visit("/bazaar");
  cy.findByText(noteTitle);
})

Then("notebook {string} is shared to the Bazaar", (noteTitle) => {
  cy.request({
      method: "POST",
      url: "/api/testability/share_to_bazaar",
      body: { noteTitle }
  }) .then((response) => {
    expect(response.status).to.equal(200);
  });
});

Then("there shouldn't be any note edit button for {string}", (noteTitle) => {
  cy.findNoteCardEditButton(noteTitle).should("not.exist");
});

When("I open the notebook {string} in the Bazaar", (noteTitle) => {
  cy.findByText(noteTitle).click();
});

When("I go to the bazaar", () => {
  cy.visit("/bazaar");
});

When("I subscribe to notebook {string} in the bazaar, with target of learning {int} notes per day", (noteTitle, count) => {
  cy.visit("/bazaar");
  cy.subscribeToNote(noteTitle, count);
});

Then("I should not see the {string} button on notebook {string}", (btnTitle, noteTitle) => {
  cy.findNoteCardButton(noteTitle, btnTitle).should("not.exist");
});

Then("I should see readonly notebook {string} in my notes", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, "edit note").should("not.exist");
});

Then("I should see I've subscribed to {string}", (noteTitle) => {
  cy.findByText(noteTitle).should("be.visible");
});

Then("I should see I've not subscribed to {string}", (noteTitle) => {
  cy.findByText("Subscribed Notes").should("exist");
  cy.findByText(noteTitle).should("not.exist");
});

Then("I should be able to edit the subscription to notebook {string}", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, "Edit subscription").click();
  cy.findByRole('button', {name: "Update"}).click();
});

When("I change notebook {string} to skip review", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, "Edit notebook settings").click();
  cy.getFormControl("Skip Review Entirely").check();
  cy.findByRole('button', {name: "Update"}).click();
});

Then("I should see it has link to {string}", (noteTitle) => {
  cy.findByText(noteTitle, {selector: ".link-title"}).click();
  cy.findByText(noteTitle, { selector: ".h1" }).should("be.visible");
});

Then("I unsubscribe from notebook {string}", (noteTitle) => {
  cy.unsubscribeFromNotebook(noteTitle);
  cy.findByRole('button', { name: 'OK' }).click();
});



