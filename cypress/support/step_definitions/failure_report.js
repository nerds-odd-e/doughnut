import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("I've logged in as {string}", (externalIdentifier) => {
  cy.loginAs(externalIdentifier);
});

When("I open the {string} set address bar", (url) => {
  cy.visit('http://localhost:8081/' + url)
});

Given("I choose to share my note {string}", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".share-card").click();
})

Then("I should see {string} is shared in the Bazaar", (noteTitle) => {
  cy.visit("/bazaar");
  cy.findByText(noteTitle);
})

Then("note {string} is shared to the Bazaar", (noteTitle) => {
  cy.request({
      method: "POST",
      url: "/api/testability/share_to_bazaar",
      body: { noteTitle }
  }) .then((response) => {
    expect(response.status).to.equal(200);
  });
});

Then("there shouldn't be any note edit button for {string}", (noteTitle) => {
  cy.findNoteCardEditButton(noteTitle).should("not.exist");
});

When("I open the detail {string} in the top bar", (noteTitle) => {
  cy.findByText(noteTitle).click();
});



Given("I've logged in as an existing user", () => {
  cy.loginAs('old_learner');
});

Given("I've logged in as another existing user", () => {
  cy.loginAs('another_old_learner');
});

When("I identify myself as a new user", () => {
  cy.visit("/login");

  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get("form.form-signin").submit();
  cy.location("pathname", { timeout: 10000 }).should("eq", "/");
});

When("I should be asked to create my profile", () => {
  cy.get("body").should("contain", "Please create your profile");
});

When("I save my profile with:", (data) => {
  data.hashes().forEach((elem) => {
    for (var propName in elem) {
      cy.getFormControl(propName).type(elem[propName]);
    }
  });
  cy.get('input[value="Submit"]').click();
});

Then("I should see {string} in the page", (content) => {
  cy.get("body").should("contain", content);
});

Then("My name {string} is in the top bar", (name) => {
  cy.get("nav").should("contain", name);
});

Then("my daily new notes to review is set to {int}", (number) => {
  cy.updateCurrentUserSettingsWith({ daily_new_notes_count: number });
});

Then("my space setting is {string}", (number) => {
  cy.updateCurrentUserSettingsWith({ space_intervals: number });
});

Then("I haven't login", () => {
});


When("I open the note {string} in the Bazaar", (noteTitle) => {
  cy.findByText(noteTitle).click();
});

When("I open the note {string} in the Bazaar in article view", (noteTitle) => {
  cy.findByText(noteTitle).click();
  cy.findByRole('button', {name: "Article View"}).click();
});

When("I should see in the article:", (data) => {
  data.hashes().forEach(({level, title}) => {
      cy.findByText(title, {selector: '.'+level}).should("be.visible");
  });
});

When("I should not see {string} in the article", (content) => {
  cy.findByText(content).should("not.exist");
});

When("I go to the bazaar", () => {
  cy.visit("/bazaar");
});

When("I should see two bullets in the article", () => {
  cy.get("body").find('li.article-view').should('have.length', 2);
});

When("I should see {string} as non-title in the article", (content) => {
      cy.findByText(content, {selector: '.note-body'});
});



When("I subscribe to note {string} in the bazaar, with target of learning {int} notes per day", (noteTitle, count) => {
  cy.visit("/bazaar");
  cy.subscribeToNote(noteTitle, count);
});

Then("I should not see the {string} button on note {string}", (btnClass, noteTitle) => {
  cy.findNoteCardButton(noteTitle, "." + btnClass).should("not.exist");
});

Then("I should see the {string} button on note {string}", (btnClass, noteTitle) => {
  cy.findNoteCardButton(noteTitle, "." + btnClass).should("exist");
});

Then("I should see readonly note {string} in my notes", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".edit-card").should("not.exist");
});

Then("I should see I've subscribed to {string}", (noteTitle) => {
  cy.findByText(noteTitle).should("be.visible");
});

Then("I should be able to edit the subscription to note {string}", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".edit-subscription").click();
  cy.findByRole('button', {name: "Update"}).click();
});

When("I change notebook {string} to skip review", (noteTitle) => {
  cy.visitMyNotebooks();
  cy.findNoteCardButton(noteTitle, ".edit-notebook").click();
  cy.getFormControl("SkipReviewEntirely").check();
  cy.findByRole('button', {name: "Update"}).click();
});

Then("I should see it has link to {string}", (noteTitle) => {
  cy.findByText(noteTitle, {selector: ".badge a"}).click();
  cy.findByText(noteTitle, { selector: ".h1" }).should("be.visible");
});

