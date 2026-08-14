@mockBrowserTime
@disableOpenAiService
Feature: Overlap try again
  As a learner doing spelling recall
  I want to be asked for a more specific answer when my answer is correct but non-distinguishing because of declared overlap
  So that I get no SRS credit and can retry the same review

  Background:
    Given I am logged in as an existing user
    And I have a notebook "Overlap practice" with notes:
      | Title   | Content                  |
      | Overlap |                          |
      | colour  | Colour means a hue       |
      | Partner | Partner note body        |
    And the notes "Overlap, Partner" are skipped from the assimilation sequence
    And note "colour" has content:
      """
      ---
      aliases:
        - color
      overlaps:
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
    And It's day 1
    And the note "colour" was assimilated as spelling on day 1

  Scenario: Shared non-distinguishing answer shows overlap try-again without credit
    Given the note "Partner" was assimilated as spelling on day 2
    And I visit the spelling memory tracker for "Partner"
    And I record the current memory tracker schedule for "Partner"
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "colour"
    Then I should see an overlap try-again alert for spelling
    And I should not see matched notes or accidental match on the overlap result
    And the spelling memory tracker for "Partner" should keep its recorded schedule

  Scenario: Try again then distinguishing plain alias credits as correct
    When I visit recall for a due quiz question on day 2
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "colour"
    Then I should see an overlap try-again alert for spelling
    When I try the spelling question again
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "color"
    Then I should see that my last answer to spelling question is correct
