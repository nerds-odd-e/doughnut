Feature: Authentication with Token

  @ignore
  Scenario: Authenticate user with a valid token
    Given a user exists
    And I have a token "my_token"
    When I access the secure endpoint of MCP with the token "my_token"
    Then the request should be successful
    And the user can be authenticated

  @ignore
  Scenario: Fail to authenticate user with an invalid token
    Given a user exists
    And I have an invalid token "invalid_token"
    When I access the secure endpoint of MCP with the token "invalid_token"
    Then the request should fail
    And the user cannot be authenticated

  @ignore
  Scenario: User can manage their tokens after login
    Given I am logged in
    When I request to create a token
    Then I should receive a new token
    And I can use this token to authenticate with MCP

  @ignore
  Scenario: User can view their existing tokens
    Given I am logged in
    And I have an existing token
    When I view my tokens
    Then I should see a list of all my active tokens