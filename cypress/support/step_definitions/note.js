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
  cy.visit("/note");

  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      cy.get(`[data-cy="${propName}"]`).type(elem[propName]);
    }
    cy.get('input[value="Submit"]').click();
  });
});

Then("Reviews should include note page with title {string} and description {string}", (noteTitle, noteDescription) => {
  cy.location("pathname", { timeout: 10000 }).should("eq", "/review");

  const lookUp = (countDown, history, callback) => {
    if (countDown === 0) {
      expect(history).to.include(noteTitle);
      return;
    }
    var domElement = cy.get(`[data-cy="note-title"]`);
    domElement.invoke("text").then(text=>{
      if(text === noteTitle) {
        cy.get(`[data-cy="note-description"]`).should("contain", noteDescription);
        return;
      }
      cy.findByText("Next").click();
      callback(countDown - 1, history.add(text), callback);
    });
  };
  lookUp(5, new Set(), lookUp);
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

Then("I have some notes", (data) => {
  let notes = data?.hashes().map((item) => {
    return {
      title: item["note-title"],
      description: item["note-description"],
      updatedDatetime: item["note-updatedDateTime"]
    }
  })
  cy.seedNotes(notes);
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
