import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I choose to share my note {string}", (noteTitle) => {
  cy.visit("/notes");
  cy.findNoteCardButton(noteTitle, ".share-card").click();
})

Then("I should see {string} is shared in the Bazaar", (noteTitle) => {
  cy.visit("/bazaar");
  cy.findByText(noteTitle);
})

Then("note {string} is shared to the Bazaar", (noteTitle) => {
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

When("I open the note {string} in the Bazaar", (noteTitle) => {
  cy.findByText(noteTitle).click();
});

When("I open the note {string} in the Bazaar in article view", (noteTitle) => {
  cy.findByText(noteTitle).click();
  cy.findByRole('button', {name: "Article View"}).click();
});

When("I should see in the article:", (data) => {
  data.hashes().forEach(({level, title}) => {
      cy.findByText(title, {selector: '.'+level}).should("be.visible");
  });
});

When("I go to the bazaar", () => {
  cy.visit("/bazaar");
});


When("I subscribe to note {string} in the bazaar, with target of learning {int} notes per day", (noteTitle, count) => {
  cy.visit("/bazaar");
  cy.findNoteCardButton(noteTitle, ".add-to-learning").click();
  cy.get("#subscription-dailyTargetOfNewNotes").type(count);
  cy.findByRole('button', {name: "Add to my learning"}).click();
});

Then("I should not see the {string} button on note {string}", (btnClass, noteTitle) => {
  cy.findNoteCardButton(noteTitle, "." + btnClass).should("not.exist");
});

Then("I should see the {string} button on note {string}", (btnClass, noteTitle) => {
  cy.findNoteCardButton(noteTitle, "." + btnClass).should("exist");
});













