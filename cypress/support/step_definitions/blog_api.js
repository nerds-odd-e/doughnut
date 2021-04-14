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
    cy.get("body").should("contain", "content");
});