// ***********************************************
// custom commands and overwrite existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

import '@testing-library/cypress/add-commands'

Cypress.Commands.add("cleanDBAndSeedData", () => {
  cy.request("/api/testability/clean_db_and_seed_data").its("body").should("contain", "OK");
});

Cypress.Commands.add("loginAs", (username) => {
  const password = "password";
  cy.request({
    method: "POST",
    url: "/login",
    form: true,
    body: { username, password }
  }) .then((response) => {
    expect(response.status).to.equal(200);
  });
});

Cypress.Commands.add("seedNotes", (notes) => {
  cy.request({method: "POST", url: "/api/testability/seed_notes", body: notes})
  .then((response) => {
     expect(response.body.length).to.equal(notes.length);
     const titles = notes.map(n=>n["title"]);
     const noteMap = Object.assign({}, ...titles.map((t, index) => ({[t]: response.body[index]})));
     cy.wrap(noteMap).as("seededNoteIdMap");
  })
})

Cypress.Commands.add("submitNoteFormWith", (notes) => {
  notes.forEach((elem) => {
    for (var propName in elem) {
      if (elem[propName]) {
        cy.get(`#${propName}`).clear().type(elem[propName]);
      }
    }
    cy.get('input[value="Submit"]').click();
  });
});

Cypress.Commands.add("expectNoteCards", (expectedCards) => {
  expectedCards.forEach((elem) => {
    for (var propName in elem) {
         cy.findByText(elem[propName]).should("be.visible");
    }
  });
});

Cypress.Commands.add("navigateToNotePage", (noteTitlesDividedBySlash) => {
  cy.visit("/notes");
  noteTitlesDividedBySlash.commonSenseSplit("/").forEach(noteTitle => cy.findByText(noteTitle).click());
});

// jumptoNotePage is faster than navigateToNotePage
//    it uses the note id memorized when creating them with testability api
Cypress.Commands.add("jumpToNotePage", (noteTitle) => {
  cy.get('@seededNoteIdMap').then(seededNoteIdMap=>
    cy.visit(`/notes/${seededNoteIdMap[noteTitle]}`)
  );
});

Cypress.Commands.add("clickButtonOnCardBody", (noteTitle, buttonTitle) => {
    const card = cy.findByText(noteTitle, { selector: ".card-title a"});
    const button = card.parent().parent().findByText(buttonTitle);
    button.click();
});

Cypress.Commands.add("creatingLinkFor", (noteTitle) => {
    cy.visit("/notes");
    cy.findNoteCardButton(noteTitle, ".link-card").click();
});

Cypress.Commands.add("expectExactLinkTargets", (targets) => {
    targets.forEach((elem) => {
         cy.findByText(elem, {selector: '.card-title a'}).should("be.visible");
    });
    cy.findAllByText(/.*/, {selector: '.card-title a'}).should("have.length", targets.length);
});

Cypress.Commands.add("findNoteCardButton", (noteTitle, selector) => {
  return cy.findByText(noteTitle).parent().parent().parent().find(selector);
});

Cypress.Commands.add("updateCurrentUserSettingsWith", (hash) => {
  cy.request({
    method: "POST",
    url: "/api/testability/update_current_user",
    body: hash
  }).its("body").should("contain", "OK")
});

Date.prototype.addDays = function(days) {
    var date = new Date(this.valueOf());
    date.setDate(date.getDate() + days);
    return date;
}

Cypress.Commands.add("timeTravelTo", (day, hour) => {
  const travelTo = new Date(1976, 5, 1, hour).addDays(day);
  cy.request({
    method: "POST",
    url: "/api/testability/time_travel",
    body: { travel_to: JSON.stringify(travelTo) }
  }).its("body").should("contain", "OK")
});

Cypress.Commands.add("initialReviewOneNoteIfThereIs", ({review_type, title, additional_info}) => {
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
                const [expectedDescription, expectedPicture] = additional_info.commonSenseSplit("; ")
                cy.get('#note-description').should("contain", expectedDescription);
                cy.get('#note-picture').find('img').should('have.attr', 'src').should('include',expectedPicture);
            }
            break;
        }

        case "related notes": {
            if(additional_info) {
                additional_info.commonSenseSplit(", ").forEach(expectedLinkTarget =>
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

Cypress.Commands.add("repeatReviewOneNoteIfThereIs", ({review_type, title, additional_info}) => {
    cy.initialReviewOneNoteIfThereIs({review_type, title, additional_info});
});
