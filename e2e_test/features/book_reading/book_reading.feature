@withCliConfig
@interactiveCLI
@mockMineruLib
Feature: Book reading

  Background:
    Given I am logged in as an existing user
    And I have a notebook with the head note "Top Maths"
    And I have a valid Doughnut Access Token with label "for cli"
    And I add the saved access token in the interactive CLI using add-access-token
    When I attach book "refactoring.pdf" to the notebook "Top Maths" via the CLI
    And I open the book attached to notebook "Top Maths"

  Scenario: See book structure and beginning of PDF in the browser
    Then I should see the book structure in the browser:
      | 0 | Main Topic 1 |
      | 1 | Subtopic 1.1 |
      | 1 | Subtopic 1.2 |
      | 0 | Main Topic 2 |
      | 1 | Subtopic 2.1 |
      | 1 | Subtopic 2.2 |
    And I should see the beginning of the PDF book "refactoring.pdf"
    And jumping between outline rows on the same page should scroll the PDF to different positions

  Scenario: Outline row jumps the PDF to the anchored page
    When I choose the book outline row "Main Topic 2"
    Then I should see PDF page 2 marker "Strengthening the Code" in the book reader
    And the book outline row "Main Topic 2" should be selected in the book reader

  Scenario: Scrolling the PDF updates the viewport-current outline row
    When I scroll the PDF book reader to bring page 2 into primary view
    Then I should see PDF page 2 marker "Strengthening the Code" in the book reader
    And the book outline row "Subtopic 2.2" should be viewport-current in the book reader

  Scenario: Short viewport scrolls outline aside so viewport-current row stays visible
    When I set the book reading viewport to 1200 by 280
    And I scroll the PDF book reader to bring page 2 into primary view
    Then I should see PDF page 2 marker "Strengthening the Code" in the book reader
    And the book outline row "Subtopic 2.2" should be viewport-current and visible in the outline aside

  Scenario: Same-page scroll moves viewport-current; selected outline row stays the explicit choice
    When I choose the book outline row "Subtopic 1.1"
    Then the book outline row "Subtopic 1.1" should be selected in the book reader
    And the book outline row "Subtopic 1.1" should be viewport-current in the book reader
    When I scroll the PDF book reader down within the same page to move viewport past the next outline bbox
    Then the book outline row "Subtopic 1.1" should be selected in the book reader
    And the book outline row "Subtopic 1.2" should be viewport-current in the book reader
