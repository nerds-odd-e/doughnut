import {
  Given,
  And,
  Then,
  When,
  Before
} from 'cypress-cucumber-preprocessor/steps';

Given('There is a blog titled {string} in Doughnut', title => {
  cy.visit('/notebooks/new_blog');
  cy.submitNoteFormsWith([{ Title: title }]);
});

When(
  'I add a new blog post in {string} on {string} with title {string}',
  (blogTitle, date, articleTitle) => {
    cy.visitMyNotebooks();
    cy.navigateToNotePage(blogTitle);
    cy.addBlogPost([{ Title: articleTitle, date }]);
  }
);

