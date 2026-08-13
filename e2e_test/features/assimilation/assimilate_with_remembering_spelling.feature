@disableOpenAiService
Feature: Assimilate with remembering spelling
  As a learner, I want to assimilate notes with spelling verification.
  Spelling is only available for notes with content.

  Background:
    Given I am logged in as an existing user
    And I have a notebook "English practice" with notes:
      | Title   |
      | English |
    And the notes "English" are skip-recalled

  Scenario Outline: Remember spelling verifies the title
    Given I have a notebook "English practice" with notes:
      | Title        | Content             |
      | <note_title> | Non-empty body text |
    And I am assimilating the note "<note_title>"
    And I remember spelling
    When I verify spelling with "<spelling_input>"
    Then the spelling verification result for note "<note_title>" should be <expected_result>

    Examples:
      | note_title    | spelling_input | expected_result         |
      | sedition      | sedition       | "success"               |
      | sedition      | wrong answer   | "error: wrong spelling" |
      | colour／color | colour／color  | "success"               |
      | colour／color | color          | "error: wrong spelling" |

  Scenario: Verify spelling accepts frontmatter alias
    Given I have a notebook "English practice" with notes:
      | Title  |
      | colour |
    And note "colour" has content:
      """
      ---
      aliases:
        - color
      ---
      Non-empty body text
      """
    And I am assimilating the note "colour"
    And I remember spelling
    When I verify spelling with "color"
    Then the spelling verification result for note "colour" should be "success"

  Scenario: Remember spelling creates a spelling tracker without assimilating
    Given I have a notebook "English practice" with notes:
      | Title | Content             |
      | Word  | Non-empty body text |
    When I am assimilating the note "Word"
    And I remember spelling
    And I verify spelling with "Word"
    Then I should see a spelling memory tracker
    And assimilate should be enabled
    And I should be assimilating the note "Word"
