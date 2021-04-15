import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

Given("odd-e blog という Blog Notebookがある", (data) => {
     cy.loginAs('developer');

     cy.visitMyNotebooks();
     cy.findByText("Add New Notebook").click();
     cy.submitNoteFormWith(data.hashes());
});

And("how to do Scrum という Blogがpostされている", (data) => {
    cy.findByText("(Add Child Note)").click();
    cy.submitNoteFormWith(data.hashes());
});

When("サードパーティアプリから odd-e blog api を使う", () => {
});

Then("サードパーティアプリから how to do Scrum というブログが見られる", () => {
});


var responseJsonObj;
When("odd-e blog api をリクエストすると", () => {
    cy.request('http://localhost:8081/api/note/blog').then((response) => {
        responseJsonObj = response.body;
    });
});

Then("ノートオブジェクトが取得できること", () => {
    cy.readFile('cypress/support/step_definitions/api_note.json').then(obj => {
        cy.wrap(obj).its('title').should("eq", responseJsonObj.title);
        cy.wrap(obj).its('description').should("eq", responseJsonObj.description);
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