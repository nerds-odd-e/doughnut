import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given("There is a Blog Notebook called odd-e blog", (data) => {
  cy.loginAs('developer');

  cy.visitMyNotebooks();
  cy.findByText("Add New Notebook").click();
  cy.submitNoteFormWith(data.hashes());
});

And("A blog called how to do Scrum is posted", (data) => {
    cy.findByText("(Add Child Note)").click();
    cy.submitNoteFormWith(data.hashes());
});

When("Use odd-e blog api from a third party app", () => {
    cy.visit("http://localhost:8081/sample/blog_viewer_sample.html");
    cy.get("#app > div:nth-child(1) > input[type=button]:nth-child(2)").click();
});

Then("You can see a blog called how to do Scrum from a third party app", () => {
    cy.get("#app > div:nth-child(2) > p:nth-child(2)").should('have.text', "how to do Scrum");
    cy.get("#app > div:nth-child(2) > p:nth-child(4)").should('have.text', "Scrum");
    cy.get("#app > div:nth-child(2) > p:nth-child(6)").should('have.text', "Developer");
    cy.get("#app > div:nth-child(2) > p:nth-child(8)").invoke('text').should('match', /\d\d\d\d-\d\d-\d\d \d\d:\d\d:\d\d.+/);
});

var responseJsonObj;
When("Request an odd-e blog api", () => {
    cy.request('http://localhost:8081/api/note/blog').then((response) => {
        responseJsonObj = response.body;
    });
});

Then("Can get the note object", () => {
    cy.fixture("json/api_note.json").then(apiNote => {
      expect(apiNote.title).to.equal(responseJsonObj.title);
      expect(apiNote.description).to.equal(responseJsonObj.description);
    });
});

/*
When("APIアドレスにNoteIdが渡ってアクセスされたとき", () => {
  cy.visit('http://localhost:8081/api/note/860')
});

Then("Noteの内容が取得できる", () => {
    cy.get("body").should("contain", "blogblog");
/*
    cy.route({
      method: 'GET',
      url: 'http://localhost:8081/api/note/860',
      onRequest: (xhr) => {
        // Example assertion
        expect(xhr.request.body.data).to.deep.equal({success:true});
      }
    })*/
/*
    cy.readFile('cypress/support/step_definitions/api_note.json')
      .should('deep.eq', cy.get("body"));
});
*/
