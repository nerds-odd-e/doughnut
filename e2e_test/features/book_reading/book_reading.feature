Feature: Book reading

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"

  @ignore
  Scenario: Attach PDF and see structure in the browser
    When I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
    Then I should see the book structure of the notebook "Top Maths" in the browser
