import { Given , And , Then , When} from "cypress-cucumber-preprocessor/steps";

Given('There is a Github user with', (data) => {
  cy.log(data);
});

When('I login using Github account {string} successfully', (email) => {
  cy.visit('/');

  cy.get('a')
    .contains('login with Github')
    .then(($a) => {
      const url = $a.prop('href')
      cy.request(({url: url, followRedirect: false})).its('redirectedToUrl').should('contain', 'https://github.com/login/oauth/authorize')
    })
});

When('I should be asked to create my profile', () => {
});

When('I save my profile without changing anything', () => {
});

When('Account for {string} should have', (email, data) => {
});
