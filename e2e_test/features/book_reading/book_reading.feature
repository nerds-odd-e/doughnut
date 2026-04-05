@withCliConfig
@interactiveCLI
@mockMineruLib
Feature: Book reading

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    And I have a valid Doughnut Access Token with label "for cli"
    And I add the saved access token in the interactive CLI using add-access-token

  Scenario: Attach PDF and see structure in the browser
    Given I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
    When I open the book attached to notebook "Top Maths"
    Then I should see the book structure in the browser:
      | 0 | Main Topic 1 |
      | 1 | Subtopic 1.1 |
      | 1 | Subtopic 1.2 |
      | 0 | Main Topic 2 |
      | 1 | Subtopic 2.1 |
      | 1 | Subtopic 2.2 |
    And I should see the beginning of the PDF book "top-maths.pdf"

  Scenario: Outline row jumps the PDF to the anchored page
    Given I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
    When I open the book attached to notebook "Top Maths"
    And I choose the book outline row "Main Topic 2"
    Then I should see PDF page 2 marker "DOUGHNUT_E2E_BOOK_PAGE2" in the book reader
    And the book outline row "Main Topic 2" should be selected in the book reader

  Scenario: Same-page outline entries with different bboxes scroll to different positions
    Given I attach book "top-maths.pdf" to the notebook "Top Maths" via the CLI
    When I open the book attached to notebook "Top Maths"
    Then same-page bbox outline entries should produce distinct PDF scroll positions
