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

Then("Reviews should include note page with title {string} and description {string}", (noteTitle, noteDescription) => {

  cy.visit('/review');
  const lookUp = (history, forOccurrence, callback) => {
    cy.get(`[data-cy="note-title"]`).invoke("text").then(currNoteTitle=>{
      const newHistory = [...history, currNoteTitle];
      if(forOccurrence(newHistory, currNoteTitle)) {
        return;
      }
      cy.findByText("Next").click();
      callback(newHistory, forOccurrence, callback);
    });
  };
  const recursiveLookUp = (forOccurrence) => lookUp([], forOccurrence, lookUp);

  const maxReviewLookUpCount = 5;
  recursiveLookUp(
      (history, currentNoteTitle) => {
           if (history.length === maxReviewLookUpCount) {assert.isTrue(false, `${history.join(", ")} to contain ${noteTitle}`);}
           if(currentNoteTitle === noteTitle) {
                cy.get(`[data-cy="note-description"]`).should("contain", noteDescription);
                return true;
           }
           return false;
      }
  );
})

Then("Reviews should include note page with:", (data) => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/review");
  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      var domElement = cy.get(`[data-cy="${propName}"]`);
      domElement.should("contain", elem[propName])
    }
  });
})

Then("I should see the note with title and description on the review page", (data) => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/review");
  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      var domElement = cy.get(`[data-cy="${propName}"]`);
      domElement.should("contain", elem[propName])
    }
  });
})

When("I review my notes", () => {
  cy.visit("/review")
})

Then("I click on next note", () => {
  cy.findByText("Next").click();
})

Given("I link Sedition to Sedation", (data) => {
  const notes = data.hashes().map((noteData) => ({
    title: noteData["note-title"],
    description: noteData["note-description"]
  }));

  cy.seedNotes(notes).then((response) => {
    const [sourceId, targetId] = response.body;

    cy.linkNote(sourceId, targetId)
  });
})


Then("I should see following note with links on the review page", (data) => {
  expect(cy.findByText(data.hashes()[0]["note-title"])).to.exist
})
