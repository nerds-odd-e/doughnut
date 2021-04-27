import {
  Given,
  And,
  Then,
  When,
  Before
} from "cypress-cucumber-preprocessor/steps";

Given("There is a blog titled {string} in Doughnut", (title) => {
  cy.visit('/notebooks/new_blog');
  cy.submitNoteFormsWith([{Title:title}]);
});


Then("I should see year {int} on the blog-site's side navbar", (year)=>{
    cy.get('.yearList').children().contains(`${year}`).should('have.length', 1);
});

When("I open the Blog page", () => {
  cy.visitBlog();
});

When("I add a new blog post in {string} on {string} with title {string}", (blogTitle, date, articleTitle) => {
    cy.visitMyNotebooks();
    cy.navigateToNotePage(blogTitle);
    cy.addBlogPost([{Title: articleTitle, date}]);
});

Then("I should see a blog post on the Blog page", (data) => {
    cy.assertBlogPostInWebsiteByTitle(
        {
            title: data.hashes()[0]['Title'],
            description: data.hashes()[0]['Description'],
            authorName: data.hashes()[0]['AuthorName'],
            createdAt: data.hashes()[0]['Date']
        }
    );
});

