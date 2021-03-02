import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("Reviews should have review pages in sequence:", (data) => {
  data.hashes().forEach(reviewPage => {

    switch(reviewPage["review type"]) {
    case "single note": {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        cy.get('#note-description').should("contain", reviewPage["additional info"]);
        break;
    }

    case  "picture note": {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        const [expectedDescription, expectedPicture] = reviewPage["additional info"].split("; ")
        cy.get('#note-description').should("contain", expectedDescription);
        cy.get('#note-picture').find('img').should('have.attr', 'src').should('include',expectedPicture);
        break;
    }

    case "related notes": {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        reviewPage["additional info"].split(", ").forEach(expectedLinkTarget =>
            cy.findByText(expectedLinkTarget, {selector: '#note-links li'})
        )
    }

    case "end of review": {
        cy.findByText("You have done all the reviews for today.").should("be.visible");
        break;
    }
    default:
        expect(true).to.be.false("unknown view page type");
    }

    cy.findByText("Next").click();
  });
})

