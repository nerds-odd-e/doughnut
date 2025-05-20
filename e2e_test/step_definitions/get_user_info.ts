import { Given, When, Then } from '@badeball/cypress-cucumber-preprocessor'

let _mcpToken = '1234567890'

Given('header contains {string}', (tokenType: string) => {
  if (tokenType === 'none') {
    _mcpToken = ''
  } else if (tokenType === 'exist') {
    // Set a valid token for existing user, adjust as needed
    _mcpToken = 'valid-existing-token'
  } else if (tokenType === 'non_exist') {
    // Set a token that represents a non-existing user
    _mcpToken = 'non-existing-token'
  }
})

When('the client requests user information via MCP service', () => {
  cy.request({
    method: 'GET',
    url: '/api/user/info', // Adjust the URL to the actual MCP user info endpoint
    headers: {
      mcpToken: _mcpToken,
    },
    failOnStatusCode: false,
  }).as('userInfoResponse')
})

Then('the username should be returned', () => {
  cy.get('@getUsernameResponse').then((response) => {
    expect(response.status).to.eq(200)
    expect(response.body).to.have.property('userName')
    expect(response.body.userName).to.be.a('string').and.not.be.empty
  })
})
