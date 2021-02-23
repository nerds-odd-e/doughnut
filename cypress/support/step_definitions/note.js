import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("there are some notes for the current user", (data) => {
  cy.seedNotes(data.hashes());
})

When("I create note with:", (data) => {
  cy.createNotes(data.hashes());
});

Then("Reviews should have review pages in sequence:", (data) => {
  data.hashes().forEach(reviewPage => {
    if (reviewPage["review type"] === "single note") {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        cy.get('#note-description').should("contain", reviewPage["additional info"]);
    }
    if (reviewPage["review type"] === "related notes") {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        reviewPage["additional info"].split(", ").forEach(expectedLinkTarget =>
            cy.findByText(expectedLinkTarget, {selector: '#note-links li'})
        )
    }
    cy.findByText("Next").click();
  });
})

