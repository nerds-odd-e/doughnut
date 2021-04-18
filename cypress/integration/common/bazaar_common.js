import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Then("note {string} is shared to the Bazaar", noteTitle => {
  cy.request({
    method: "POST",
    url: "/api/testability/share_to_bazaar",
    body: { noteTitle }
  }).then(response => {
    expect(response.status).to.equal(200);
  });
});

Then("I should see {string} is shared in the Bazaar", noteTitle => {
  cy.visit("/bazaar");
  cy.findByText(noteTitle);
});

When("I open the note {string} in the Bazaar in article view", noteTitle => {
  cy.findByText(noteTitle).click();
  cy.findByRole("button", { name: "Article View" }).click();
});

Then("I should see it has link to {string}", noteTitle => {
  cy.findByText(noteTitle, { selector: ".badge a" }).click();
  cy.findByText(noteTitle, { selector: ".h1" }).should("be.visible");
});

Then(
  "I should be able to edit the subscription to note {string}",
  noteTitle => {
    cy.visitMyNotebooks();
    cy.findNoteCardButton(noteTitle, ".edit-subscription").click();
    cy.findByRole("button", { name: "Update" }).click();
  }
);

When("I should see in the article:", data => {
  data.hashes().forEach(({ level, title }) => {
    cy.get("." + level)
      .contains(title)
      .should("be.visible");
  });
});
