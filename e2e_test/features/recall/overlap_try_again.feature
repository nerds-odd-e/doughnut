@mockBrowserTime
@disableOpenAiService
Feature: Overlap try again
  As a learner doing spelling recall
  I want to be asked for a more specific answer when my answer is correct but non-distinguishing because of declared overlap
  So that I get no SRS credit and can retry the same review

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Overlap practice" with notes:
      | Title   | Content                  | Skip Memory Tracking | Remember Spelling |
      | Overlap |                          | true                 |                   |
      | colour  | Colour means a hue       |                      | true              |
      | Partner | Partner note body        | true                 |                   |
    And note "colour" has content:
      """
      ---
      aliases:
        - color
        - "[[Partner]]"
      ---
      Colour means a hue
      """
    And note "Partner" has content:
      """
      ---
      aliases:
        - colour
      ---
      Partner note body
      """

  Scenario: Shared non-distinguishing answer shows overlap try-again without credit
    Given It's day 1
    And the note "colour" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "colour"
    Then I should see an overlap try-again alert for spelling
    And I should not see matched notes or accidental match on the overlap result

  Scenario: Try again then distinguishing plain alias credits as correct
    Given It's day 1
    And the note "colour" was assimilated on day 1
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "colour"
    Then I should see an overlap try-again alert for spelling
    When I click overlap try again
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "color"
    Then I should see that my last answer to spelling question is correct
