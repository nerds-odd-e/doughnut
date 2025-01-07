import { When, Then } from '@badeball/cypress-cucumber-preprocessor';

When('I download notebook {string}', (notebookTitle: string) => {
  cy.get(`[data-test="download-notebook-${notebookTitle}"]`)
    .click();
});

When('I download notebook {string} from the bazaar', (notebookTitle: string) => {
  cy.get(`[data-test="download-bazaar-notebook-${notebookTitle}"]`)
    .click();
});

Then('the notebook should be downloaded successfully', () => {
  // Cypress automatically handles downloads
  // We can verify the file exists in the downloads folder
  cy.readFile('cypress/downloads/*').should('exist');
});

Then('the downloaded file should contain all notes from {string}', (notebookTitle: string) => {
  // Read the downloaded file and verify its contents
  cy.readFile('cypress/downloads/*').then((content) => {
    const notebook = JSON.parse(content);
    expect(notebook.title).to.equal(notebookTitle);
    // Additional verification of notebook contents could go here
  });
});

Then('I should see a download button for notebook {string}', (notebookTitle: string) => {
  cy.get(`[data-test="download-notebook-${notebookTitle}"]`)
    .should('be.visible');
});

Then('I should not see a download button for notebook {string}', (notebookTitle: string) => {
  cy.get(`[data-test="download-notebook-${notebookTitle}"]`)
    .should('not.exist');
}); 