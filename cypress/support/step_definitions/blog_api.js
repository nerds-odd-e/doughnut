import {
  Given,
  And,
  Then,
  When,
  Before,
} from "cypress-cucumber-preprocessor/steps";

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
    cy.readFile('cypress/support/step_definitions/api_note.json')
      .should('deep.eq', cy.get("body"));
});