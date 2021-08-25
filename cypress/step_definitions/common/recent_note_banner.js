When("I should see new note banner", () => {
  cy.findByText("This note has been changed recently.");
});

Then("I should see 0 new note banner", () => {
  cy.findByText("This note has been changed recently.").should(
    "have.length",
    0
  );
});
