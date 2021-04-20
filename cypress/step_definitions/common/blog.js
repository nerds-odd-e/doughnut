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

And("A blog is posted in {string}", (blogNotebook, data) => {
    cy.findByText("(Add Child Note)").click();
    cy.submitNoteFormWith(data.hashes());
});

When("Use odd-e blog api from a third party app", () => {
    cy.visit("http://localhost:8081/sample/blog_viewer_sample.html");
    cy.get("#app > div:nth-child(1) > input[type=button]:nth-child(2)").click();
});

Then("You can see a blog {string} from a third party app", (blogTitle) => {
    cy.get("#app > div:nth-child(2) > p:nth-child(2)").should('have.text', blogTitle);
    cy.get("#app > div:nth-child(2) > p:nth-child(4)").should('have.text', "Scrum");
    cy.get("#app > div:nth-child(2) > p:nth-child(6)").should('have.text', "Developer");
    cy.get("#app > div:nth-child(2) > p:nth-child(8)").invoke('text').should('match', /\d\d\d\d-\d\d-\d\d \d\d:\d\d:\d\d.+/);
});

Given("There is no Notebook type blog", () => {

});

When("I open the blog page", () => {
  cy.visitBlog();
});

Then("the left panel should show empty", () => {
  cy.getYearList().should('be.empty');
});

Given("There is a Notebook type blog with title 'odd-e-blog'", () => {

});


Then("the left panel should show Years list", () => {
  cy.get('.yearList').should(($lis) => {
  expect($lis).to.have.length(5)
  });
});