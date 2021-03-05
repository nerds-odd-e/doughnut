import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Then("I do these initial reviews in sequence:", (data) => {
  cy.visit('/review');
  data.hashes().forEach(({review_type, title, additional_info}) => {
    if(review_type == "end of review") {
        cy.findByText("You have done all the reviews for today.").should("be.visible");
    }
    else {
        cy.findByText(title, {selector: '#note-title'})
        switch(review_type) {
        case "single note": {
            if(additional_info) {
                cy.get('#note-description').should("contain", additional_info);
            }
            break;
        }

        case  "picture note": {
            if(additional_info) {
                const [expectedDescription, expectedPicture] = additional_info.split("; ")
                cy.get('#note-description').should("contain", expectedDescription);
                cy.get('#note-picture').find('img').should('have.attr', 'src').should('include',expectedPicture);
            }
            break;
        }

        case "related notes": {
            if(additional_info) {
                additional_info.split(", ").forEach(expectedLinkTarget =>
                    cy.findByText(expectedLinkTarget, {selector: '#note-links li'})
                )
            }
            break;
        }

        default:
            expect(review_type).equal("a known review page type");
        }

        cy.findByText("Next").click();
    }
  });
})

Given("It's day {int}, {int} hour", (day, hour) => {
    cy.timeTravelTo(day, hour);
});

Then("I should be able to follow these review actions:", (data) => {
  data.hashes().forEach(({day, old_notes_to_review, initial_review}) => {
    cy.timeTravelTo(day, 8);
    cy.log(initial_review)
  });
});

