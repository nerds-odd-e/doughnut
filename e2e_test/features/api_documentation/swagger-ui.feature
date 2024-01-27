Feature: Browse and use swagger-ui

  Scenario: User logout because of session timeout
    Given I am logged in as an existing user
    When I visit the swagger-ui
    Then I can browse the publicly exposed REST APIs
