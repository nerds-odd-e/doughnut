@mockBrowserTime
@disableOpenAiService
Feature: Overlap try again
  As a learner doing spelling recall
  I want to retry when my answer names a declared overlap
  So that I can recall the requested note while keeping my review schedule

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
        - color
        - hue
      ---
      Partner note body
      """
    And It's day 1
    And the note "colour" was assimilated as spelling on day 1

  Scenario Outline: Retry an overlap answer and receive credit for the requested note
    When I visit recall for a due recall prompt on day 2
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "<overlap>"
    Then I should see an overlap try-again alert for spelling
    When I try the spelling question again
    Then I should be asked spelling question "means a hue" from notebook "Overlap practice"
    When I type my answer "<answer>"
    Then I should see that my last answer to spelling question is correct

    Examples:
      | overlap | answer |
      | Partner | colour |
      | hue     | color  |
