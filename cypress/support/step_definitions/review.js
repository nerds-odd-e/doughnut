import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("Reviews should have review pages in sequence:", (data) => {
  cy.visit('/review');
  data.hashes().forEach(reviewPage => {
    const additionalInfo = reviewPage["additional info"];
    switch(reviewPage["review type"]) {
    case "single note": {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        if(additionalInfo) {
            cy.get('#note-description').should("contain", additionalInfo);
        }
        break;
    }

    case  "picture note": {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        if(additionalInfo) {
            const [expectedDescription, expectedPicture] = additionalInfo.split("; ")
            cy.get('#note-description').should("contain", expectedDescription);
            cy.get('#note-picture').find('img').should('have.attr', 'src').should('include',expectedPicture);
        }
        break;
    }

    case "related notes": {
        cy.findByText(reviewPage["title"], {selector: '#note-title'})
        if(additionalInfo) {
            additionalInfo.split(", ").forEach(expectedLinkTarget =>
                cy.findByText(expectedLinkTarget, {selector: '#note-links li'})
            )
        }
        break;
    }

    case "end of review": {
        cy.findByText("You have done all the reviews for today.").should("be.visible");
        return;
    }
    default:
        expect(true).to.be.false("unknown view page type");
    }

    cy.findByText("Next").click();
  });
})

Then("Review in sequence", (data) => {
  data.hashes().forEach(reviewActionsOfADay => {
    cy.timeTravelTo(reviewActionsOfADay["day"], 8);
  });
});

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

