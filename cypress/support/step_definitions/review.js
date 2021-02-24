import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("Reviews should have review pages in sequence:", (data) => {
  data.hashes().forEach(reviewPage => {

    if (reviewPage["review type"] === "single note") {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        cy.get('#note-description').should("contain", reviewPage["additional info"]);
    }

    if (reviewPage["review type"] === "picture note") {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        const [expectedDescription, expectedPicture] = reviewPage["additional info"].split("; ")
        cy.get('#note-description').should("contain", expectedDescription);
        cy.get('#note-picture').find('img').should('have.attr', 'src').should('include',expectedPicture);
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

