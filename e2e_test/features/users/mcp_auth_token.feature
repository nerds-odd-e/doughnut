Feature: Authentication with Token

  Scenario: Authenticate user with a valid token
    Given a user exists
    And I have a token "my_token"
    When I access the secure endpoint of MCP with the token "my_token"
    Then the request should be successful
    And the user can be authenticated

  Scenario: Fail to authenticate user with an invalid token
    Given a user exists
    And I have an invalid token "invalid_token"
    When I access the secure endpoint of MCP with the token "invalid_token"
    Then the request should fail
    And the user cannot be authenticated