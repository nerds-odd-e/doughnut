@withCliConfig
@interactiveCLI
@mockMineruLib
Feature: Book reading

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    And I have a valid Doughnut Access Token with label "for cli"
    When I add the saved access token in the interactive CLI using add-access-token

  Scenario: Attach PDF and see structure in the browser
    When I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
    Then I should see the book structure of the notebook "Top Maths" in the browser:
      | 0 | Main Topic 1 |
      | 1 | Subtopic 1.1 |
      | 1 | Subtopic 1.2 |
      | 0 | Main Topic 2 |
      | 1 | Subtopic 2.1 |
      | 1 | Subtopic 2.2 |
    And I should be able to download the attached book PDF matching fixture "top-maths.pdf"
    And I should see the beginning of the PDF book "top-maths.pdf"
 