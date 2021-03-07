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

When("I should see the article {string} with sub-content {string}", (title, contents) => {
  cy.findByText(title, {selector: '.h1'}).should("be.visible");
  contents.commonSenseSplit(",").forEach(content =>
      cy.findByText(content, {selector: '.h2'}).should("be.visible")
  )
});



