import { Given , And , Then , When} from "cypress-cucumber-preprocessor/steps";

When('I identify myself as a new user', () => {
  cy.visit('/login');

  cy.get("#username").type("user");
  cy.get("#password").type("password");
  cy.get('form.form-signin').submit();
  cy.location('pathname', { timeout: 10000 }).should('eq', '/');
  cy.get('input[type="submit"][value="Logout"]').should('be.visible');
});

When('I should be asked to create my profile', () => {
  cy.get("body").should('contain', 'Please create your profile');
});

When('I save my profile with:', (data) => {
  data.hashes().forEach(elem =>{
    for(var propName in elem) {
      cy.get("#" + propName).type(elem[propName]);
    }
  });

});

When('Account for {string} should have', (email, data) => {
});
