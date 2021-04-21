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

Given("There is a notebook titled {string} with type Blog in Doughnut", (title) => {
  cy.loginAs('developer');
  cy.visitMyNotebooks();
  cy.findByText("Add New Notebook").click();
  //TODO need to set the notebook type to blog
  cy.submitNoteFormWith([{Title:title}]);
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
And("There are some notes in the notebook", () => {
    cy.findByText("(Add Child Note)").click();
    cy.submitNoteFormWith([{Title:"Hello World", Description: "Hello World"}]);
});

Then("I should see a blog post titled {string} on the Blog page", (title) => {
    cy.findArticleInWebsiteByTitle(title).should('be.visible');
});



When("after creating a blog article {string}", (articleTitle, data) => {
  cy.findByText("(Add Child Note)").click();
  cy.submitNoteFormWith(data.hashes());
});

Then("I should see {string} in breadcrumb", (expectedBreadcrumb) => {
  const d = new Date();
  const y = d.getFullYear();
  const m = d.toLocaleString("default", {month: 'short'});
  const bc = expectedBreadcrumb.replace("{YYYY}", y).replace("{MMM}", m);

  cy.get('.breadcrumb').within( ()=>
      bc.commonSenseSplit(", ").forEach(s => cy.findByText(s))
  );
});